package com.firfi.slackbaka.workers

import akka.actor.{ActorSystem, ActorRef}
import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding
import akka.http.scaladsl.marshallers.xml.ScalaXmlSupport
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.model.headers.Cookie
import akka.http.scaladsl.unmarshalling._
import akka.stream.{ActorMaterializer, Materializer}
import akka.stream.scaladsl.{Flow, Sink, Source}
import com.firfi.slackbaka.SlackBaka.{PrivateResponse, ChatMessage}
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.api.indexes.{IndexType, Index}
import reactivemongo.bson.{BSONDocumentReader, BSONDateTime, BSONDocument}
import slack.api.SlackApiClient
import scala.concurrent.{ExecutionContext, Future}
import ExecutionContext.Implicits.global


import scala.concurrent.Future
import scala.util.{Try, Failure, Success, Random}

object NomadLoader extends BakaLoader {
  override def getWorkers: Set[Class[_]] = {
    Set(classOf[NomadWorker])
  }
}

case class MongoGeorecord(user: String, city: String)

object MongoGeorecord {
  implicit object MongoGeorecordReader extends BSONDocumentReader[MongoGeorecord] {
    def read(doc: BSONDocument): MongoGeorecord = {
      def gets(n: String) = doc.getAs[String](n).get
      MongoGeorecord(gets("user"), gets("city"))
    }
  }
}

case class Geoname(name: String, lat: Double, long: Double)

class NomadWorker(responder: ActorRef) extends BakaRespondingWorker(responder) with ScalaXmlSupport with MongoExtension {

  implicit val system = ActorSystem("Nomad")
  implicit val materializer: Materializer = ActorMaterializer()

  val nomadCities = db.map((db_) => db_[BSONCollection]("nomadCities"))

  lazy val connectionFlow: Flow[HttpRequest, HttpResponse, Any] =
    Http().outgoingConnection("api.geonames.org")
  implicit val geonamesUnmarshaller: FromEntityUnmarshaller[Seq[Geoname]] =
    defaultNodeSeqUnmarshaller
      .map(_ \ "geoname").map(l => l.map(p => (p \ "name", p \ "lat", p \ "lng"))
      .map({case (name, lat, lng) => Geoname(name.text, lat.text.toDouble, lng.text.toDouble)}))
  val geonamesUsername = System.getenv("GEONAMES_USERNAME")
  val setCityPattern = """baka nomad city set (.+)""".r
  val getCityVillagersPatten = """baka nomad city get (.+)""".r
  val helpPattern = """baka nomad help""".r
  val cityListPattern = """baka nomad city list"""
  def request(query: String): Future[HttpResponse] = {
    val cityFeatureClass = "P"
    import com.netaporter.uri.dsl._
    Source.single(RequestBuilding.Get(
      ("/search?" ? ("q" -> query)
        & ("featureClass" -> cityFeatureClass)
        & ("maxRows" -> 1)
        & ("username" -> geonamesUsername)).toString
    )).via(connectionFlow).runWith(Sink.head)
  }
  def getGeoname(city: String): Future[Either[String, Option[Geoname]]] = request(city).flatMap {
    case HttpResponse(OK, _, entity, _) => Unmarshal(entity).to[Seq[Geoname]].map(seq =>
      Right(seq.headOption)
    )
    case HttpResponse(status, _, entity, _) => Unmarshal(entity).to[String].map { entity =>
      val error = s"Geonames request failed with status code $status and entity $entity"
      Left(error)
    }
  }
  def checkGeoname(city: String): Future[Either[String, Either[String, Geoname]]] = getGeoname(city).map { r =>
    r.right.map {
      case Some(geoname) if geoname.name == city => Right(geoname)
      case Some(geoname) => Left(s"City name $city isn't in database. You meant ${geoname.name}?")
      case None => Left(s"No city with name $city found")
    }
  }
  def setNomadCity(cm: ChatMessage, geoname: Geoname): Future[Unit] = {
    for {
      c <- tryToFuture(nomadCities)
      r <- c.update(
        BSONDocument("user" -> cm.user),
        BSONDocument(
          "user" -> cm.user,
          "city" -> geoname.name
        ),
        upsert = true
      )
    } yield {}
  }
  def getNomadCity(geoname: Geoname): Future[List[MongoGeorecord]] = {
    import MongoGeorecord._
    for {
      c <- tryToFuture(nomadCities)
      r <- c.find(BSONDocument("city" -> geoname.name)).cursor[MongoGeorecord]().collect[List]()
    } yield r
  }
  def checkGeonameThen(cityName: String, f: (Geoname) => Future[Either[Unit, String]]): Future[Either[Unit, String]] =
    checkGeoname(cityName).flatMap {
      case Right(either) => either match {
        case Left(errorMessageForUser) => Future.successful(Right(errorMessageForUser))
        case Right(geoname) => f(geoname)
      }
      case Left(e) => Future.successful(Left(e))
    }
  override def handle(cm: ChatMessage): Future[Either[Unit, String]] = {
    cm.message match {
      case setCityPattern(cityName) => checkGeonameThen(cityName,
        (geoname) => setNomadCity(cm, geoname).map(_ => Right(s"City ${geoname.name} set."))
      )
      case getCityVillagersPatten(cityName) => checkGeonameThen(cityName,
        (geoname) => getNomadCity(geoname).map(nomads => {
          nomads match {
            case n :: ns => {
              responder ! PrivateResponse((List(s"Nomads in city ${geoname.name}:") :: nomads.map(_.user.toSlackMention)).mkString("\n"), cm.user)
              Right(s"Nomads in city ${geoname.name} sent to your PM. Nomads count: ${nomads.length}")
            }
            case _ => {
              Right(s"There's no nomads in city ${geoname.name}")
            }
          }
        }).recoverWith {
          case e: Exception => println(e); Future.successful(Left(e))
        }
      )
      case _ => Future { Left() }
    }
  }
}
