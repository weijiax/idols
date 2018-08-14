package utils

import play.api.libs.json._
import scala.collection.mutable.HashMap

object AccountAllocator {
  var trainingAccounts: HashMap[JsValue, Int] = HashMap()

  // mapping from idols users account to tacc account
  var accountMapping: HashMap[String, String] = HashMap()

  def init(json: JsValue) = {
    var index = 0
    while ((json \ "training_accounts" \ index).isInstanceOf[JsDefined]) {
      trainingAccounts += ((json \ "training_accounts" \ index).get -> 0)
      index += 1
    }
  }

  def allocate = {
    var taccName: String = ""
    var taccPassword: String = ""

    var found = false
    val keyIt = trainingAccounts.keysIterator
    while (!found && keyIt.hasNext) {
      var key = keyIt.next
      var value = trainingAccounts.get(key).getOrElse(-1)
      if (value == 0) {
        trainingAccounts.put(key, 1)
        taccName = (key \ "username").as[String].replace("\"", "")
        taccPassword = (key \ "password").as[String].replace("\"", "")
        found = true;
      }
    }
    (taccName, taccPassword)
  }

  def map(user: String, tacc: String) {
    accountMapping += (user -> tacc)
  }
}