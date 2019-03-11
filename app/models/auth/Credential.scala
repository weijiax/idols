package models.auth

import play.api.libs.json._

abstract class Credential(json: JsValue) {
  //  val firstName = (json \ "firstName").as[String].replace("\"", "")
  //  val lastName = (json \ "lastName").as[String].replace("\"", "")
  val username = (json \ "username").as[String].replace("\"", "")
}