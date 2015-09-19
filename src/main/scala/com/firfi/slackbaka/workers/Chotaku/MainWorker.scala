package com.firfi.slackbaka.workers.chotaku

import com.firfi.slackbaka.SlackBaka.ChatMessage

import akka.actor.ActorRef

import scala.concurrent.Future
import scala.concurrent._
import ExecutionContext.Implicits.global

class MainWorker(responder: ActorRef) extends AbstractWorker(responder) {

  val SEARCH_ROOT = API_ROOT + "read/art/list?"
  val DETAILS_ROOT = API_ROOT + "read/art?"

  // This worker also processes plain "чотач" command
  override def isOwner(cm: ChatMessage): Boolean = {
    super.isOwner(cm) || cm.message == BASE_PREFIX
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

  override def process(cm: ChatMessage): Future[Either[Unit, String]]  = {
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
  }
}
