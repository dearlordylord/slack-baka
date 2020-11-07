package com.firfi.slackbaka.workers

import com.firfi.slackbaka.SlackBaka.ChatMessage
import reactivemongo.api.MongoConnection.ParsedURIWithDB
import reactivemongo.api.bson.collection.BSONCollection
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.api.bson.{BSONDateTime, BSONDocument}

import scala.concurrent.Future
import reactivemongo.api._

import scala.concurrent.ExecutionContext.Implicits.global

object HistoryLoader extends BakaLoader {
  override def getWorkers: Set[Class[_]] = {
    Set(classOf[HistoryWorker])
  }
}

object MongoExtension {
  val driver = new AsyncDriver
  val uri: String = System.getenv("MONGO_URI")
  val connection: Future[(ParsedURIWithDB, MongoConnection)] = MongoConnection.fromStringWithDB(uri).flatMap { parsedUri =>
    driver.connect(parsedUri).map(c => {
      println("connected to db")
      (parsedUri, c)
    }).recover({
      case e =>
        println("Error getting db connection")
        println(e)
        throw e
    })
  }
}

trait MongoExtension {
  val getDb = () => MongoExtension.connection.flatMap(c => c._2.database(c._1.db)).recover({
    case e =>
      println("Error getting db from connection")
      println(e)
      throw e
  })
}

class HistoryWorker(none: Any) extends BakaWorker with MongoExtension {


  override def handle(cm: ChatMessage) = {
    val messages: Future[BSONCollection] = getDb().map(db_ => db_[BSONCollection]("messages"))
    /*
    `The second are the message timestamps, which always use the "ts" name (eg: "ts", "event_ts"). These are returned as strings. Even though they look like floats, they should always be stored and compared as strings - because floats occasionally lose precision and that would be bad here.`
    `A message in a channel will have a unique TS for that channel (two messages in two different channels can have the same TS)`
     */
    for {
      c <- messages
      _ <- c.indexesManager.ensure(Index(Seq("ts"->IndexType.Ascending, "channel"->IndexType.Ascending), unique=true))
      _ <- c.indexesManager.ensure(Index(Seq("message"->IndexType.Text)))
      _ <- c.indexesManager.ensure(Index(Seq("timestamp"->IndexType.Ascending)))
    } {
      val timestamp = cm.ts.split('.')(0).toLong * 1000
      c.insert.one(BSONDocument(
        "message" -> cm.message,
        "user" -> cm.user,
        "channel" -> cm.channel,
        "ts" -> cm.ts,
        "timestamp" -> BSONDateTime(timestamp)
      ))
    }
  }
}
