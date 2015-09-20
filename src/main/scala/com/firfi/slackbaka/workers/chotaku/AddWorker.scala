package com.firfi.slackbaka.workers.chotaku

import com.firfi.slackbaka.SlackBaka.ChatMessage

import akka.actor.ActorRef

import scala.concurrent.Future
import scala.concurrent._
import scala.util.matching.Regex
import ExecutionContext.Implicits.global

class AddWorker(responder: ActorRef) extends AbstractWorker(responder) {

  val UPLOAD_ROOT = API_ROOT + "upload/art?"
  val ADD_ROOT = API_ROOT + "create/art?"

  def getModuleRegex: Regex = {
    new Regex("""добавь\s*<(https?://.*)(?:>|\|)""", "url")
  }

  case class NotFoundException(url: String) extends Exception
  case class AlreadyExistException(id: String) extends Exception

  override def process(cm: ChatMessage, params: Regex.Match): Future[Either[Unit, String]] = {
    request(UPLOAD_ROOT, Map("file" -> params.group("url"))).map((res) => {
      val response = new ResponseUpload(res)
      response.file.get("error") match {
        case Some(error) =>
          if (response.file("error_code") == 30)
            throw new AlreadyExistException(response.file("error_text").asInstanceOf[String])
          else
            throw new NotFoundException(params.group("url"))
        case _ => response.file("upload_key")
      }
    }).flatMap((key) => {
      request(ADD_ROOT, Map("upload_key" -> key.asInstanceOf[String]))
    }).map((res) => {
      val response = new ResponseBasic(res)
      response.error match {
        case Some(error) =>
          if (error("code") == 30)
            throw new AlreadyExistException(error("message").asInstanceOf[String])
          else
            throw new Exception
        case _ => Right("Успешно добавлено под номером " + response.parsed("id"))
      }
    }).recover {
      case NotFoundException(url) => Right("Не удалось скачать файл " + url)
      case AlreadyExistException(id) => Right("Арт уже есть под номером " + id)
      case _ => Right("Произошла неизвестная ошибка, приносим свои извинения")
    }
  }
}