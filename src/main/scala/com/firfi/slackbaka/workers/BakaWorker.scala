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
  def dispatch(response: String): Unit = {
    log.info(s"dispatching response $response")
  }
  def receive = {
    case ChatMessage(message, channel) =>
      workers.map((w) => w ! ChatMessage(message, channel))
    case BakaResponse(message, channel) =>
      responder ! BakaResponse(message, channel)
  }
}

trait BakaWorker extends Actor {
  def handle(message: String): Future[Either[Unit, String]]
  def receive = {
    case ChatMessage(message, channel) =>
      val back = sender()
      handle(message).map {
        case Left(_) => println("left")
        case Right(response) =>
          back ! BakaResponse(response, channel)
      }
  }
}


