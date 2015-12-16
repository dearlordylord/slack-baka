package com.firfi.slackbaka.listeners

import akka.actor.{ActorRef, Actor}
import com.firfi.slackbaka.SlackBaka.{PrivateResponse, BakaResponse}
import slack.api.SlackApiClient
import slack.models.{UserTyping, User, TeamJoin}

class WelcomeListener(responder: ActorRef) extends Actor {
  override def receive: Receive = {
    case TeamJoin(user: User) => responder ! PrivateResponse(
      s"Hey ${user.name} and welcome to our community!", user.id
    )
    // case UserTyping(channel: String, user: String) => responder ! PrivateResponse("test", "USERIDHERE")
  }
}