package com.firfi.slackbaka.listeners

import akka.actor.{ActorRef, Actor}
import com.firfi.slackbaka.SlackBaka.BakaResponse
import slack.models.{User, TeamJoin}

class WelcomeListener(responder: ActorRef, generalId: String) extends Actor {
  override def receive: Receive = {
    case TeamJoin(user: User) => responder ! BakaResponse(
      s"Hey ${user.name} and welcome to our community!", generalId
    )
  }
}