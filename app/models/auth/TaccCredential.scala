package models.auth

import play.api.libs.json._
import scala.sys.process._
import models.JupyterNotebook
import scala.collection.mutable.ListBuffer

class TaccCredential(json: JsValue) extends Credential(json) {
  val password = (json \ "password").as[String].replace("\"", "")
  var token: String = ""
  var jobs = scala.collection.mutable.HashSet[String]()

  def getToken(): String = {
    if (token.equals("")) {
      // Get access token
      // encode special characters (% and &)
      val tempPassword: String = password.replaceAll("%", "%25").replaceAll("&", "%26")
      var cmd = Seq("curl", "-X", "POST", "-u", "_zVxwGJfexDkmSnUT1e7y2mLYAIa:IUokd8ceXpoPuwvNpgnOm4bB0_ga", "-d",
        "grant_type=password", "-d", s"username=$username", "-d", s"password=$tempPassword", "-d", "scope=PRODUCTION",
        "https://api.tacc.utexas.edu/token")

      // execute curl command and retrieve response
      var response = cmd.!!

      // user authorized, use access_token get user profile by executing another curl command
      token = (Json.parse(response) \ "access_token").as[String].replace("\"", "")
    }
    return token
  }

  def setToken(t: String) {
    token = t
  }

  def getJobStatus(): String = {
    // json to be displayed to user on screen
    var js: StringBuffer = new StringBuffer

    val command1 = "squeue -n jupyter -u " + username
    var response = Process(Seq("bash", "-c", command1)).!!.split("\n")
    if (response.length < 2) {
      js.append("[")
      for (i <- 0 until 3) {
        js.append("\"\",")
      }
      js.append("\"" + username + "\",")
      for (i <- 3 until 9) {
        js.append("\"\",")
      }
      js.append("\"\"")
      js.append("],")
    } else {
      val notebooks = utils.NotebookAllocator.taccMapping.getOrElse(username, ListBuffer())

      for (i <- 1 until response.length) {
        js.append(response(i).trim().split("\\s+").mkString("[\"", "\",\"", "\""))
        if (i - 1 < notebooks.length) {
          for (n <- notebooks) {
            js.append(",\"" + n.port + "\"" + ",\"" + n.password + "\"")
          }

        } else {
          js.append(",\"\",\"\"")
        }
        js.append("],")
      }

    }
    js.toString()
  }

}