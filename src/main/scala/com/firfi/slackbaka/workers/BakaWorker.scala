package com.firfi.slackbaka.workers

import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.headers.{`Content-Type`, GenericHttpCredentials, Authorization}
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.{PredefinedFromEntityUnmarshallers, Unmarshal}
import akka.stream.{ActorMaterializer, Materializer}
import akka.stream.scaladsl.{Sink, Source, Flow}
import com.firfi.slackbaka.SlackBaka.{BakaResponse, ChatMessage}
import org.jboss.netty.handler.codec.marshalling.DefaultUnmarshallerProvider
import spray.json.DefaultJsonProtocol

import scala.concurrent.Future

import akka.actor.{ActorSystem, ActorRef, Actor}
import akka.event.Logging
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success, Try, Random}

abstract class BakaLoader {
  def getWorkers:Set[Class[_]]
}

class BakaDispatcher(workers: Set[ActorRef]) extends Actor {
  val log = Logging(context.system, this)
  def receive = {
    case cm@ChatMessage(message, channel, user, ts) =>
      workers.map((w) => w ! cm)
  }
}

object ImgurUtility extends ImgurUtility

trait ImgurUtility extends SprayJsonSupport with DefaultJsonProtocol {

  import scala.concurrent.{ ExecutionContext, Future, Promise, Await }
  import scala.concurrent.ExecutionContext.Implicits.global
  import spray.json._

  private implicit val system = ActorSystem("Gelbooru")
  private val IMGUR_ID = System.getenv("IMGUR_ID")
  private val IMGUR_SECRET = System.getenv("IMGUR_SECRET")
  private val scheme = "https://"
  private val rootUrl = "api.imgur.com"
  private val imageUploadEndpoint = "/3/image"

  private lazy val connectionFlow: Flow[HttpRequest, HttpResponse, Any] =
    Http().outgoingConnection(rootUrl)

  /*

   */

  // case class BasicResponse(data: Any, )

  private implicit val testUnmarshaller = PredefinedFromEntityUnmarshallers.stringUnmarshaller
  private implicit val materializer: Materializer = ActorMaterializer()

  /*
  {"data":{"id":"aKlNT6k","title":null,"description":null,"datetime":1454228976,"type":"image\/png","animated":false,"width":581,"height":834,"size":454109,"views":0,"bandwidth":0,"vote":null,"favorite":false,"nsfw":null,"section":null,"account_url":null,"account_id":0,"comment_preview":null,"deletehash":"pQcn1cwsgK1Tf2e","name":"","link":"http:\/\/i.imgur.com\/aKlNT6k.png"},"success":true,"status":200}
   */

  private final case class ResponseData(link: Option[String])
  private final case class Response(data: ResponseData, success: Boolean, status: Int) // http status?

  private implicit val responseDataFormat = jsonFormat1(ResponseData)
  private implicit val responseFormat = jsonFormat3(Response)

  def imgurify(url: String): Future[String] = { // TODO some images have more than 10 mb weight, return original url in such case
    for {
      request <- Marshal(FormData(("image", url))).to[RequestEntity]
      response <- Http().singleRequest(
        HttpRequest(method = HttpMethods.POST, uri = s"$scheme$rootUrl$imageUploadEndpoint", entity = request)
          .addHeader(Authorization(GenericHttpCredentials("Client-ID", IMGUR_ID)))
      )
      entity <- Unmarshal(response.entity).to[Response] // TODO it doesn't handle any errors
      res <- entity match {
        case Response(_, false, _) => Future{url}
        case Response(data, true, _) => Future{data.link.get}
      }
    } yield res
  }
}

trait BakaWorkerUtility {

  import java.net.URLEncoder

  protected def commaEnvToSet(varName: String): Set[String] = {
    commaSeparatedToSet(Option(System.getenv(varName)).getOrElse(""))
  }

  protected def commaSeparatedToSet(s: String): Set[String] = {
    s.split(",").map(_.trim).filter((s) => s.nonEmpty).toSet
  }

  // https://github.com/dispatch/reboot/issues/23#issuecomment-9663215
  protected def encodeURIComponent(s: String): String = {
    URLEncoder.encode(s, "UTF-8").
      replaceAll("\\+", "%20").
      replaceAll("\\%21", "!").
      replaceAll("\\%27", "'").
      replaceAll("\\%28", "(").
      replaceAll("\\%29", ")").
      replaceAll("\\%7E", "~")
  }

}

trait BakaWorker extends Actor with BakaWorkerUtility {
  def handle(cm: ChatMessage)

  def receive = {
    case cm@ChatMessage(text, channel, user, ts) =>
      handle(cm)
  }
}

abstract class BakaRespondingWorker(responder: ActorRef) extends Actor with BakaWorkerUtility {
  val log = Logging(context.system, this)

  implicit class ToUserMention(user: String) {
    // U024BE7LH to <@U024BE7LH> so slack clients make sense out of user and we don't need to pull user names each time
    def toSlackMention = s"<@$user>"
  }

  implicit def tryToFuture[T](t: Try[T]): Future[T] = {
    t match {
      case Success(c) => Future.successful(c)
      case Failure(e) => Future.failed(e)
    }
  }

  def handle(cm: ChatMessage): Future[Either[Unit, String]]

  def receive = {
    case cm@ChatMessage(text, channel, user, ts) =>
      handle(cm).map {
        case Left(_) => log.info("left")
        case Right(response) =>
          responder ! BakaResponse(response, channel)
      }
  }
}


