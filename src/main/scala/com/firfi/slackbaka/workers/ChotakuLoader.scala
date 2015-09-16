package com.firfi.slackbaka.workers

import com.firfi.slackbaka.SlackBaka.ChatMessage
import dispatch.{Http, url, Defaults, as}

import akka.actor.{ActorRef, Actor, Props, ActorSystem}

import scala.concurrent.Future
import scala.concurrent._
import ExecutionContext.Implicits.global

object ChotakuLoader extends BakaLoader {
  override def getWorkers:Set[Class[_]] = {
    Set(classOf[chotaku.MainWorker])
  }
}