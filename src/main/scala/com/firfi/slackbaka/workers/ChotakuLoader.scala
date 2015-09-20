package com.firfi.slackbaka.workers

object ChotakuLoader extends BakaLoader {
  override def getWorkers:Set[Class[_]] = {
    Set(
      classOf[chotaku.SearchWorker],
      classOf[chotaku.AddWorker]
    )
  }
}