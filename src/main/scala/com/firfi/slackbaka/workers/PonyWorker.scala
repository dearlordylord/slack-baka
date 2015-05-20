package com.firfi.slackbaka.workers

import dispatch.{Http, url, Defaults, as}

import scala.concurrent.Future
import scala.concurrent._
import ExecutionContext.Implicits.global


class PonyWorker extends BakaWorker {

  val API_ROOT = "https://derpiboo.ru/"

  def request(api: String, query: String = ""): Future[String] = {
    val path = api + ".json?" + query
    val svc = url(API_ROOT + path)
    Http(svc OK as.String)
  }

  def searchQuery(tags: Seq[String], params: Map[String, String] = Map.empty): String = {
    val COMMA = "%2C"
    val tagsString = tags.map((t) => {t.replace(' ', '+')}).mkString(COMMA)
    (params ++ Map[String, String](("q", tagsString))).map({case (k, v) => k + "=" + v}).mkString("&")
  }

  val randomQuery = searchQuery(Seq("-suggestive", "-explicit", "-semi-grimdark", "-grimdark", "safe"),
    Map("min_score"->"88", "random_image"->"true"))

  val pattern = """(?i).*\bпони\b.*""".r
  override def handle(text: String): Future[Either[Unit, String]] = {
    import scala.util.parsing.json._
    text match {
      case pattern() => request("search", randomQuery).map((res) => {
        // num.zero
        JSON.parseFull(res).get.asInstanceOf[Map[String, Any]]("id").toString.toFloat.toLong // TODO combinators
      }) // TODO json parse errors
      .flatMap((id) => {
        request(id.toString)
      }).map((res) => {
        "https:" + JSON.parseFull(res).get.asInstanceOf[Map[String, Any]]("image").toString
      })
      .map(Right.apply)
      case _ => Future { Left() }
    }
  }
}
