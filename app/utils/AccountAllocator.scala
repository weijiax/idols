package utils

import play.api.libs.json._
import scala.collection.mutable.Queue
import scala.collection.mutable.HashMap
import scala.collection.mutable.ListBuffer

import models.auth.Credential
import models.auth.TaccCredential

object AccountAllocator {
  var credentialMapping: HashMap[String, TaccCredential] = HashMap()
  var allTACC = scala.collection.mutable.ArrayBuffer[TaccCredential]()
  // Available TACC accounts
  var availableTACC = Queue[TaccCredential]()

  // mapping from idols user account to other credentials
  var accountMapping: HashMap[String, ListBuffer[Credential]] = HashMap()

  def initTacc(json: JsValue) = {
    var index = 0
    while ((json \ "training_accounts" \ index).isInstanceOf[JsDefined]) {
      val account = new TaccCredential((json \ "training_accounts" \ index).get)
      allTACC += account
      availableTACC += account
      credentialMapping += ((json \ "training_accounts" \ index \ "username").as[String].replace("\"", "") -> account)
      index += 1
    }
  }

  // Allocate an available TACC account
  def allocateTacc: TaccCredential = {
    return availableTACC.dequeue
  }

  // Add new credential to this User
  def map(user: String, credential: Credential) {
    if (!accountMapping.contains(user)) {
      accountMapping.put(user, ListBuffer())
    }

    accountMapping.getOrElse(user, ListBuffer()) += credential
  }

  def contains(user: String): Boolean = {
    print(accountMapping)
    return accountMapping.contains(user)
  }

  def getAll(): scala.collection.mutable.ArrayBuffer[TaccCredential] = {
    return allTACC
  }
}