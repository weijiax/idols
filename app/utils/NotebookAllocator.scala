package utils

import play.api.libs.json._
import scala.collection.mutable.Queue
import scala.collection.mutable.HashMap
import scala.collection.mutable.ListBuffer

import models.JupyterNotebook

object NotebookAllocator {
  var allNotebook = scala.collection.mutable.ArrayBuffer[JupyterNotebook]()

  // Available Notebooks
  var availableNotebook = Queue[String]()

  var usedNotebook = Queue[JupyterNotebook]()
  // mapping from tacc account to notebook session
  var taccMapping: HashMap[String, ListBuffer[JupyterNotebook]] = HashMap()
  // mapping from port number to notebook session
  var portMapping: HashMap[String, JupyterNotebook] = HashMap()

  // mapping from email to notebook session
  var accountMapping: HashMap[String, JupyterNotebook] = HashMap()

  def initJupyterNotebook(json: JsValue) = {
    var index = 0
    while ((json \ "Jupyter Notebook Sessions" \ index).isInstanceOf[JsDefined]) {
      val session = new JupyterNotebook((json \ "Jupyter Notebook Sessions" \ index).get)
      allNotebook += session
      availableNotebook += session.port
      if (!taccMapping.contains(session.username)) {
        taccMapping += (session.username -> ListBuffer())
      }
      taccMapping(session.username) += session
      portMapping += (session.port -> session)

      index += 1
    }
  }

  // Allocate an available Notebook Session
  def allocateNotebook: JupyterNotebook = {
    val port = availableNotebook.dequeue
    var notebook = portMapping(port)
    usedNotebook += notebook
    notebook.available = false
    return notebook
  }

  def removeNotebook(user: String, port: String) {
    val notebook = portMapping.remove(port).get
    taccMapping(user) -= notebook
  }

  def get(user: String): JupyterNotebook = {
    return accountMapping(user)
  }
  // Add new credential to this User
  def map(user: String, session: JupyterNotebook) {
    accountMapping(user) = session
  }

  def contains(user: String): Boolean = {
    return accountMapping.contains(user)
  }

  def getAll(): scala.collection.mutable.ArrayBuffer[JupyterNotebook] = {
    return allNotebook
  }

  def isEmpty(): Boolean = {
    return availableNotebook.isEmpty
  }

}