package models

import play.api.libs.json._

class JupyterNotebook(json: JsValue) {
  val port = (json \ "port").as[String].replace("\"", "")
  val username = (json \ "account").as[String].replace("\"", "")
  val password = (json \ "password").as[String].replace("\"", "")
  var available = true
}