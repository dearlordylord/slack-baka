package com.firfi.slackbaka

import akka.actor.{Actor, ActorSystem, Props}
import com.firfi.slackbaka.listeners.WelcomeListener
import com.firfi.slackbaka.workers.{BakaDispatcher, GelbooruLoader, HistoryLoader, NomadLoader, PonyLoader}
import slack.api.SlackApiClient
import slack.rtm.SlackRtmClient

import scala.util.{Failure, Success}
import scala.concurrent.duration._



object SlackBaka {

  case class ChatMessage(message: String, channel: String, user: String, ts: String)

  case class BakaResponse(message: String, channel: String)
  case class PrivateResponse(message: String, user: String)

  implicit val system: ActorSystem = ActorSystem.create("Baka")

  val workers: Set[Class[_]] =
    Set() ++ HistoryLoader.getWorkers ++ PonyLoader.getWorkers ++ GelbooruLoader.getWorkers ++ NomadLoader.getWorkers

  class BakaResponder(slackRtmClient: SlackRtmClient, slackApiClient: SlackApiClient) extends Actor {
    import scala.concurrent.ExecutionContext.Implicits.global
    override def receive: Receive = {
      case BakaResponse(message, channel) =>
        slackRtmClient.sendMessage(channel, message)
      case PrivateResponse(message, user) =>
        slackApiClient.openIm(user) andThen {
          case Success(c: String) =>
            slackRtmClient.sendMessage(c, message)
            slackApiClient.closeIm(c)
          case Failure(f) => {
            println("TEST")
            println(f)
          }
        }

    }
  }

  def main(args: Array[String]) {

    val API_TOKEN = System.getenv("SLACK_TOKEN")
    val client = SlackRtmClient(API_TOKEN, duration=30.seconds) // consistent timeouts on slow connection
    val apiClient = SlackApiClient(API_TOKEN)

    val responder = system.actorOf(Props(new BakaResponder(client, apiClient)))

    val dispatcher = system.actorOf(Props(new BakaDispatcher(workers.map(worker => {
      system.actorOf(Props(worker, responder))
    }))))
    client.onMessage { message => message.user match {
      case None => println(s"no user for message ${message.text}");
      case Some(user) => dispatcher ! ChatMessage(message.text, message.channel, user, message.ts)
    } }
    client.addEventListener(system.actorOf(Props(new WelcomeListener(responder))))
  }
}

