package com.firfi.slackbaka

import akka.actor.Actor.Receive
import akka.actor.{Actor, Props, ActorSystem}
import com.firfi.slackbaka.workers.{PonyWorker, BakaWorker, BakaDispatcher}
import slack.api.SlackApiClient
import slack.rtm.SlackRtmClient

// Async
import scala.concurrent.ExecutionContext.Implicits.global


object SlackBaka {
  
  case class ChatMessage(message: String, channel: String)
  case class BakaResponse(message: String, channel: String)

  val workers = Set(classOf[PonyWorker]) // TODO find a better way to autoload it

  class BakaResponder(slackRtmClient: SlackRtmClient) extends Actor {
    override def receive: Receive = {
      case BakaResponse(message, channel) =>
        println(message)
        println(channel)
        slackRtmClient.sendMessage(channel, message)
    }
  }

  def main(args: Array[String]) {
    implicit val system = ActorSystem("Baka")
    val client = SlackRtmClient(System.getenv("SLACK_TOKEN"))

    val dispatcher = system.actorOf(Props(new BakaDispatcher(workers.map((worker) => {
      system.actorOf(Props(worker))
    }), system.actorOf(Props(new BakaResponder(client))))))
    client.onMessage { message =>
      dispatcher ! ChatMessage(message.text, message.channel)
    }
    println("Hello, world! " + args.toList)
  }
}

