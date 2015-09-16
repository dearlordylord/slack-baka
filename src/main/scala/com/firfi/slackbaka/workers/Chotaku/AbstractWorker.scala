package com.firfi.slackbaka.workers.chotaku

import com.firfi.slackbaka.workers.{chotaku, BakaRespondingWorker}
import com.firfi.slackbaka.SlackBaka.ChatMessage

import dispatch.{Http, url, Defaults, as}

import akka.actor.ActorRef

import scala.concurrent.Future
import scala.concurrent._
import ExecutionContext.Implicits.global


abstract class AbstractWorker(responder: ActorRef) extends BakaRespondingWorker(responder) {

  val BASE_PREFIX = "чотач"
  val MODULE_PREFIX = ""
  val API_ROOT = "http://api.4otaku.org/"
  val IMAGE_ROOT = "http://images.4otaku.org/art/"

  val chotakuAllowedChannels: Set[String] = commaEnvToSet("CHOTAKU_ALLOWED_CHANNELS")

  def request(api: String, params: Map[String, String] = Map.empty): Future[String] = {
    val query = params.map({case (k, v) => k + "=" + v}).mkString("&")
    val svc = url(api + query)
    Http(svc OK as.String)
  }

  def parse(res: String): Map[String, String] = {
    import scala.util.parsing.json._

    val data = JSON.parseFull(res).get.asInstanceOf[Map[String, List[Any]]]("data")
    data.head.asInstanceOf[Map[String, String]]
  }

  def getMessageParams(message: String): Array[String] = {
    // Clean up prefixes
    var result = message
    Set(BASE_PREFIX, MODULE_PREFIX).foreach((prefix) => {
      var search = prefix + " "
      // Clean up only from beginning of the string
      if (result.indexOf(search) == 0)
        result = result.substring(search.length)
    })
    result.split(" ")
  }

  def isOwner(cm: ChatMessage): Boolean = {
    val check = BASE_PREFIX + " " + MODULE_PREFIX + " "
    if (cm.message.indexOf(check) == 0)
      true
    else
      false 
  }

  def process(cm: ChatMessage): Future[Either[Unit, String]]

  override def handle(cm: ChatMessage) = {
    if (chotakuAllowedChannels.contains(cm.channel) && isOwner(cm)) {
      process(cm)
    } else {
      Future { Left() }
    }
  }
}
