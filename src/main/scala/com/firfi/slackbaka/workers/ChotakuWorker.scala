package com.firfi.slackbaka.workers

import com.firfi.slackbaka.SlackBaka.ChatMessage
import dispatch.{Http, url, Defaults, as}

import scala.concurrent.Future
import scala.concurrent._
import ExecutionContext.Implicits.global


class ChotakuWorker extends BakaWorker {

  val API_ROOT = "http://api.4otaku.org/"
  val SEARCH_ROOT = API_ROOT + "read/art/list?"
  val DETAILS_ROOT = API_ROOT + "read/art?"
  val IMAGE_ROOT = "http://images.4otaku.org/art/"

  // comma-separated channel _IDS_
  val chotakuAllowedChannels: Set[String] = commaEnvToSet("CHOTAKU_ALLOWED_CHANNELS")

  def request(api: String, params: Map[String, String] = Map.empty): Future[String] = {
    val query = params.map({case (k, v) => k + "=" + v}).mkString("&")
    val svc = url(api + query)
    Http(svc OK as.String)
  }

  def parse(res: String): Map[String, String] = {
    import scala.util.parsing.json._

    val data = JSON.parseFull(res).get.asInstanceOf[Map[String, List[Any]]]("data")
    data.apply(0).asInstanceOf[Map[String, String]]
  }

  // TODO: learn a little more about scala and implement filters as an objects
  // compiling to strings depending on sequence number
  // Like ChotakuFilter("state", "is", "approved")
  val randomQuery = Map(
    "per_page"->"1",
    "sort_by"->"random",
    "filter[0][name]"->"state",
    "filter[0][type]"->"is",
    "filter[0][value]"->"approved",
    "filter[1][name]"->"state",
    "filter[1][type]"->"is",
    "filter[1][value]"->"tagged",
    "filter[2][name]"->"art_tag",
    "filter[2][type]"->"not",
    "filter[2][value]"->"nsfw",
    "filter[3][name]"->"art_tag",
    "filter[3][type]"->"not",
    "filter[3][value]"->"loli",
    "filter[4][name]"->"art_rating",
    "filter[4][type]"->"more",
    "filter[4][value]"->"1"
  )

  val pattern = """(?i).*\bчотач\b.*""".r
  override def handle(cm: ChatMessage): Future[Either[Unit, String]] = {
    cm.message match {
      case pattern() if chotakuAllowedChannels.contains(cm.channel) =>
        request(SEARCH_ROOT, randomQuery).map((res) => {
          parse(res)("id")
        }) // TODO json parse errors
        .flatMap((id) => {
          request(DETAILS_ROOT, Map("id" -> id))
        }).map((res) => {
          val parsed = parse(res)
          val image = if (parsed("resized") == "1") parsed("md5") + "_resize.jpg"
            else parsed("md5") + "." + parsed("ext")
          IMAGE_ROOT + image
        })
        .map(Right.apply)
      case _ => Future { Left() }
    }
  }
}
