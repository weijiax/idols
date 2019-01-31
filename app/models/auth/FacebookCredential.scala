package models.auth

import play.api.libs.json._

class FacebookCredential(json: JsValue) extends Credential(json) {
}