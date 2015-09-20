package com.firfi.slackbaka.workers.chotaku

import com.firfi.slackbaka.workers.BakaRespondingWorker
import com.firfi.slackbaka.SlackBaka.ChatMessage

import dispatch.{Http, url, as}

import akka.actor.ActorRef

import scala.concurrent.Future
import scala.concurrent._
import scala.util.matching.Regex
import ExecutionContext.Implicits.global

abstract class AbstractWorker(responder: ActorRef) extends BakaRespondingWorker(responder) {

  val BASE_PREFIX = "чотач "
  val API_ROOT = "http://api.4otaku.org/"
  val IMAGE_ROOT = "http://images.4otaku.org/art/"

  val chotakuAllowedChannels: Set[String] = commaEnvToSet("CHOTAKU_ALLOWED_CHANNELS")

  def getModuleRegex: Regex

  def request(api: String, params: Map[String, String] = Map.empty): Future[String] = {
    val query = params.map({case (k, v) => k + "=" + v}).mkString("&")
    val svc = url(api + query)
    Http(svc OK as.String)
  }

  def process(cm: ChatMessage, params: Regex.Match): Future[Either[Unit, String]]

  override def handle(cm: ChatMessage): Future[Either[Unit, String]] = {
    if (chotakuAllowedChannels.contains(cm.channel) && cm.message.indexOf(BASE_PREFIX) == 0) {
      val payload = cm.message.substring(BASE_PREFIX.length)
      val moduleMatch = getModuleRegex.findFirstMatchIn(payload)
      if (moduleMatch.nonEmpty) {
        return process(cm, moduleMatch.get)
      }
    }

    // Module was not matched by message or channel, yield left
    Future { Left() }
  }
}
