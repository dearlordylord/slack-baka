package com.firfi.slackbaka

import akka.actor.Actor.Receive
import akka.actor.{Actor, Props, ActorSystem}
import com.firfi.slackbaka.workers.{HistoryLoader, PonyLoader, ChotakuLoader, BakaWorker, BakaDispatcher}
import slack.api.SlackApiClient
import slack.rtm.SlackRtmClient

// Async
import scala.concurrent.ExecutionContext.Implicits.global


object SlackBaka {

  case class ChatMessage(message: String, channel: String, user: String, ts: String)
  case class BakaResponse(message: String, channel: String)

  val workers = Set() ++ HistoryLoader.getWorkers ++ PonyLoader.getWorkers ++ ChotakuLoader.getWorkers

  class BakaResponder(slackRtmClient: SlackRtmClient) extends Actor {
    override def receive: Receive = {
      case BakaResponse(message, channel) =>
        slackRtmClient.sendMessage(channel, message)
    }
  }

  def main(args: Array[String]) {
    implicit val system = ActorSystem("Baka")
    val client = SlackRtmClient(System.getenv("SLACK_TOKEN"))

    val responder = system.actorOf(Props(new BakaResponder(client)))

    val dispatcher = system.actorOf(Props(new BakaDispatcher(workers.map((worker) => {
      system.actorOf(Props(worker, responder))
    }))))
    client.onMessage { message =>
      dispatcher ! ChatMessage(message.text, message.channel, message.user, message.ts)
    }
  }
}

