package com.firfi.slackbaka.workers.chotaku
import scala.util.parsing.json._

class ResponseBasic(raw: String) {
  val parsed = JSON.parseFull(raw).get.asInstanceOf[Map[String, Any]]

  val errors = parsed("errors").asInstanceOf[List[Map[String, Any]]]
  val error = errors.headOption

  val success = parsed("success").asInstanceOf[Boolean]
}

class Response(raw: String) extends ResponseBasic(raw) {
  val list = parsed("data").asInstanceOf[List[Map[String, Any]]]
  val data = list.headOption

  val count = parsed("count").asInstanceOf[Double].toInt
}

class ResponseUpload(raw: String) extends ResponseBasic(raw) {
  val files = parsed("files").asInstanceOf[List[Map[String, Any]]]
  val file = files.head
}