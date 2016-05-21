package com.firfi.slackbaka.workers

import java.io.IOException

import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.xml.ScalaXmlSupport
import akka.http.scaladsl.unmarshalling._
import akka.http.scaladsl.model.headers.Cookie
import akka.stream.{ActorMaterializer, Materializer}

import com.firfi.slackbaka.SlackBaka.ChatMessage

import akka.actor.{ActorSystem, ActorRef}

import scala.concurrent.Future
import scala.concurrent._
import ExecutionContext.Implicits.global
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.http.scaladsl.model.{HttpEntity, HttpResponse, HttpRequest}
import akka.http.scaladsl.client.RequestBuilding
import akka.http.scaladsl.model.StatusCodes._

import scala.util.Random

object GelbooruLoader extends BakaLoader {
  override def getWorkers:Set[Class[_]] = {
    Set(classOf[GelbooruWorker])
  }
}

case class Post(sampleUrl: String, tags: String)

class GelbooruWorker(responder: ActorRef) extends BakaRespondingWorker(responder) with ScalaXmlSupport with ImgurUtility {

  implicit val system = ActorSystem("Gelbooru")
  implicit val materializer: Materializer = ActorMaterializer()

  val allowedChannels: Set[String] = commaEnvToSet("PONY_ALLOWED_CHANNELS")

  lazy val connectionFlow: Flow[HttpRequest, HttpResponse, Any] =
    Http().outgoingConnection("gelbooru.com")

  def request(query: String = ""): Future[HttpResponse] =
    Source.single(RequestBuilding.Get(
      s"/index.php?page=dapi&s=post&q=index&tags=$query"
    ).addHeader(Cookie(("user_id", "257946"), ("pass_hash", "6b0e0b46b7cc9f5e6d25297a9fa3fbb1634c7867")))
    ).via(connectionFlow).runWith(Sink.head)

//  def request(query: String = ""): Future[String] = {
//    def cookie(k: String, v: String) = new com.ning.http.client.cookie.Cookie(k, v, "raw...", ".gelbooru.com", "/", -1, -1, true, false)
//    Future {""}
//  }

  private def tagsQuery(extraRequestTags: Set[String]): String = {
    extraRequestTags.mkString("+")
  }

  implicit val postsUnmarshaller: FromEntityUnmarshaller[Seq[Post]] =
    defaultNodeSeqUnmarshaller.map(_ \ "post").map(l => l.map(p => (p \ "@sample_url", p \ "@tags")).map({case (url, tags) => Post(sampleUrl=url.text, tags=tags.text)}))

  case class Post(sampleUrl: String, tags: String)

  val pattern = """(?i).*\bбура\b(.*)""".r
  override def handle(cm: ChatMessage): Future[Either[Unit, String]] = {
    cm.message match {
      case pattern(tags) if allowedChannels.contains(cm.channel) =>
        val extraTags = commaSeparatedToSet(tags).map(encodeURIComponent)
        request(tagsQuery(extraTags)).flatMap((response) => {
          response.status match {
            case OK => {
              Unmarshal(response.entity).to[Seq[Post]].map(seq =>
                Random.shuffle(seq).headOption.map(post => Right(post)).getOrElse(Left("no images for you fagget"))
              ).flatMap {
                case Right(post) => {
                  imgurify(post.sampleUrl).map(s => Right(s"$s\n${post.tags}"))
                }
                case Left(e) => Future {Right(e)} // return as message for user
              }
            }
            case _ => Unmarshal(response.entity).to[String].map { entity =>
              val error = s"Gelbooru request failed with status code ${response.status} and entity $entity"
              Left(error)
            }
          }
        }).map {
          case Left(error) => {
            println(error)
            Left()
          }
          case Right(r) => Right(r)
        }
      case _ => Future { Left() }
    }
  }
}
