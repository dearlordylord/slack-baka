package com.firfi.slackbaka.workers

import akka.actor.Actor.Receive
import com.firfi.slackbaka.SlackBaka
import com.firfi.slackbaka.SlackBaka.{BakaResponder, BakaResponse, ChatMessage}

import scala.concurrent.Future

import akka.actor.{ActorRef, Actor, Props}
import akka.event.Logging
import scala.concurrent.ExecutionContext.Implicits.global

class BakaDispatcher(workers: Set[ActorRef], responder: ActorRef) extends Actor {
  val log = Logging(context.system, this)
  def receive = {
    case cm@ChatMessage(message, channel, user, ts) =>
      workers.map((w) => w ! cm)
    case BakaResponse(message, channel) =>
      responder ! BakaResponse(message, channel)
  }
}

trait BakaWorkerUtility {
  protected def commaEnvToSet(varName: String): Set[String] = {
    commaSeparatedToSet(Option(System.getenv(varName)).getOrElse(""))
  }
  protected def commaSeparatedToSet(s: String): Set[String] = {
    s.split(",").map(_.trim).filter((s) => s.nonEmpty).toSet
  }
}

trait BakaWorker extends Actor with BakaWorkerUtility {
  val log = Logging(context.system, this)
  def handle(cm: ChatMessage): Future[Either[Unit, String]]
  def receive = {
    case cm@ChatMessage(text, channel, user, ts) =>
      val back = sender()
      handle(cm).map {
        case Left(_) => log.info("left")
        case Right(response) =>
          back ! BakaResponse(response, channel)
      }
  }
}


