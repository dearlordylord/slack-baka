package com.firfi.slackbaka

import akka.actor.{Actor, Props, ActorSystem}
import com.firfi.slackbaka.listeners.WelcomeListener
import com.firfi.slackbaka.workers.{HistoryLoader, PonyLoader, BakaDispatcher}
import slack.models.SlackEvent
import slack.rtm.SlackRtmClient

// Async
import scala.concurrent.ExecutionContext.Implicits.global


object SlackBaka {

  case class ChatMessage(message: String, channel: String, user: String, ts: String)

  case class BakaResponse(message: String, channel: String)

  val workers = Set() ++ HistoryLoader.getWorkers ++ PonyLoader.getWorkers

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

    val state = client.state
    val generalId = state.getChannelIdForName("general")
    generalId.map{id => client.addEventListener(system.actorOf(Props(new WelcomeListener(responder, id))))} // TODO generalise
  }
}

