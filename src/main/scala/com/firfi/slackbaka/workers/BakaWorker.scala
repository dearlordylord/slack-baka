package com.firfi.slackbaka.workers

import com.firfi.slackbaka.SlackBaka.{BakaResponse, ChatMessage}

import scala.concurrent.Future

import akka.actor.{ActorRef, Actor}
import akka.event.Logging
import scala.concurrent.ExecutionContext.Implicits.global

abstract class BakaLoader {
  def getWorkers:Set[Class[_]]
}

class BakaDispatcher(workers: Set[ActorRef]) extends Actor {
  def receive = {
    case cm@ChatMessage(message, channel, user, ts) =>
      workers.map((w) => w ! cm)
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


