package com.firfi.slackbaka.workers

import com.firfi.slackbaka.SlackBaka.ChatMessage
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.api.indexes.{IndexType, Index}
import reactivemongo.bson.{BSONDateTime, BSONDocument}
import scala.concurrent.Future
import reactivemongo.api._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success, Try}

object HistoryLoader extends BakaLoader {
  override def getWorkers: Set[Class[_]] = {
    Set(classOf[HistoryWorker])
  }
}

trait MongoExtension {
  val MONGOLAB_DB = System.getenv("MONGOLAB_DB")
  val driver = new MongoDriver
  val uri = System.getenv("MONGOLAB_URI")

  val connection: Try[MongoConnection] =
    MongoConnection.parseURI(uri).map { parsedUri =>
      driver.connection(parsedUri)
    }
  val db = connection.map((c) => c(MONGOLAB_DB))
}

class HistoryWorker(none: Any) extends BakaWorker with MongoExtension {

  val messages = db.map((db_) => db_[BSONCollection]("messages"))

  override def handle(cm: ChatMessage) = {
    /*
    `The second are the message timestamps, which always use the "ts" name (eg: "ts", "event_ts"). These are returned as strings. Even though they look like floats, they should always be stored and compared as strings - because floats occasionally lose precision and that would be bad here.`
    `A message in a channel will have a unique TS for that channel (two messages in two different channels can have the same TS)`
     */
    for {
      c <- messages match {
        case Success(c) => Future.successful(c)
        case Failure(e) => Future.failed(e)
      }
      _ <- c.indexesManager.ensure(Index(Seq("ts"->IndexType.Ascending, "channel"->IndexType.Ascending), unique=true))
      _ <- c.indexesManager.ensure(Index(Seq("message"->IndexType.Text)))
      _ <- c.indexesManager.ensure(Index(Seq("timestamp"->IndexType.Ascending)))
    } {
      val timestamp = cm.ts.split('.')(0).toLong * 1000
      c.insert(BSONDocument(
        "message" -> cm.message,
        "user" -> cm.user,
        "channel" -> cm.channel,
        "ts" -> cm.ts,
        "timestamp" -> BSONDateTime(timestamp)
      ))
    }
  }
}
