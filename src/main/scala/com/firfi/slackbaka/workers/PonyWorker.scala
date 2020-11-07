package com.firfi.slackbaka.workers

import com.firfi.slackbaka.SlackBaka.ChatMessage
import dispatch.{Http, url, Defaults, as}

import akka.actor.ActorRef

import scala.concurrent.Future
import scala.concurrent._
import ExecutionContext.Implicits.global

import io.circe._, io.circe.generic.auto._, io.circe.parser._, io.circe.syntax._

case class PonyImageRepresentations(medium: String)
case class PonyImage(representations: PonyImageRepresentations)
case class PonyImagesResponse(images: Vector[PonyImage])

object PonyLoader extends BakaLoader {
  override def getWorkers:Set[Class[_]] = {
    Set(classOf[PonyWorker])
  }
}

class PonyWorker(responder: ActorRef) extends BakaRespondingWorker(responder) {

  val API_ROOT = "https://derpibooru.org/api/v1/json/"

  // comma-separated channek _IDS_
  val ponyAllowedChannels: Set[String] = commaEnvToSet("PONY_ALLOWED_CHANNELS")
  val extraEnvTags: Set[String] = commaEnvToSet("PONY_EXTRA_TAGS")
  val apiKey: Option[String] = Option(System.getenv("PONY_API_KEY"))

  def request(api: String, query: String = ""): Future[String] = {
    val path = api + "?" + query
    println(API_ROOT + path)
    val svc = url(API_ROOT + path)
    Http.default(svc OK as.String).recover({
      case e =>
        println("Error fetching derpibooru api posts")
        println(e)
        throw e
    })
  }

  def searchQuery(tags: Seq[String], params: Map[String, String] = Map.empty): String = {
    val COMMA = "%2C"
    val tagsString = tags.map(t => {t.replace(' ', '+')}).mkString(COMMA)
    (params ++
      Map[String, String](("q", tagsString)) ++
      apiKey.map(k => Map[String, String](("key", k))).getOrElse(Map.empty)).map({case (k, v) => k + "=" + v}).mkString("&")
  }

  private def randomQuery(extraRequestTags: Set[String]): String = {
    searchQuery(
      (extraEnvTags ++ extraRequestTags)
      .foldLeft(Seq.empty[String])({case (acc, tag) =>
        val filtered = if (tag.startsWith("-")) acc.filter(_ != tag.substring(1))
          else acc.filter(_ != s"-$tag")
        if (filtered.length == acc.length) filtered :+ tag else filtered
      }),
      Map("min_score"->"88", "random_image"->"true"))
  }

  val pattern = """(?i).*\bпони\b(.*)""".r
  override def handle(cm: ChatMessage): Future[Either[String, String]] = {
    cm.message match {
      case pattern(tagsFollowingPony) if ponyAllowedChannels.contains(cm.channel) =>
        val extraTags = commaSeparatedToSet(tagsFollowingPony).map(encodeURIComponent) // and sanitize dat shit
        request("search/posts", randomQuery(extraTags)).flatMap(res => {
          val ponyImages = decode[PonyImagesResponse](res)
          ponyImages match {
            case Left(e) =>
              println("error parsing ponyimages response")
              println(e)
              Future.failed(e)
            case Right(i) => i.images match {
              case a +: as => Future.successful(a.representations.medium)
              case _ => Future.failed(new RuntimeException("No images found"))
            }
          }
        })
        .map(Right.apply)
      case _ => Future { Left("") }
    }
  }
}
