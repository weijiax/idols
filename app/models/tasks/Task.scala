package models.tasks

import javax.inject._
import play.api.mvc._
import play.api.libs.json._

import java.nio.file._
import scala.sys.process._

import models.auth.TaccCredential

abstract class Task(json: JsValue) {
  //name of this task, example: preprocessing, data analysis, postprocessing
  val task_name = (json \ "task_name").as[String].replace("\"", "")
  // type of this task, example: fileUpload
  val task_type = (json \ "task_type").as[String].replace("\"", "")
  // access level of this task
  val access_level = if ((json \ "access_level").as[String].replace("\"", "").equals("Admin")) models.auth.Roles.AdminRole else models.auth.Roles.UserRole

  //  val command1 = "ssh -n login1 \" echo '" + credential.password.replace("$", "\\$") + "' | su - " + credential.username + " -c "

  /**
   * Return the json object of this task
   */
  def get_json(): JsValue = {
    return json
  }

  /**
   * Returns the description of this task as an HTML.
   *
   * Formats of the description: text, html, markdown.
   * The description can either be directly typed in the "content" field.
   * or saved in a file indicated by "file" field.
   */
  def get_description(): String = {
    val format = (json \ "task_description" \ "format").as[String].replace("\"", "")
    val content = (json \ "task_description" \ "content").as[String].replace("\"", "")
    val file = (json \ "task_description" \ "file").as[String].replace("\"", "")

    if (content == "") {
      // content field is empty, read from file
      val path = Paths.get(file)
      if (Files.exists(path)) {
        // Conver the content of file to HTML
        return utils.Reader.toHTML(format, new String(java.nio.file.Files.readAllBytes(path)))
      } else {
        return "Description cannot be read"
      }
    } else {
      // Conver the content to HTML
      return utils.Reader.toHTML(format, content)
    }
  }

  /**
   * Run this task
   * @param body: message requested from user
   * @return feedback to user
   */
  def run(body: AnyContent, session: Int): String

  //  def runWithTACC(body: AnyContent, user: models.auth.User): String = {
  //    val username = user.getTaccName
  //    val password = user.getTaccPassword
  //
  //    // Get access token
  //    // encode special characters (% and &)
  //    val tempPassword: String = password.replaceAll("%", "%25").replaceAll("&", "%26")
  //    var cmd = Seq("curl", "-X", "POST", "-u", "_zVxwGJfexDkmSnUT1e7y2mLYAIa:IUokd8ceXpoPuwvNpgnOm4bB0_ga", "-d",
  //      "grant_type=password", "-d", s"username=$username", "-d", s"password=$tempPassword", "-d", "scope=PRODUCTION",
  //      "https://api.tacc.utexas.edu/token")
  //
  //    // execute curl command and retrieve response
  //    var response = cmd.!!
  //
  //    if (!response.startsWith("{\"error\"")) {
  //      // user authorized, use access_token get user profile by executing another curl command
  //      val access_token = (Json.parse(response) \ "access_token").as[String].replace("\"", "")
  //    }
  //
  //    // Not sure what to do after this
  //    return run(body: AnyContent)
  //  }

  implicit def reflector(ref: AnyRef) = new {
    def getByName(name: String): Any = ref.getClass.getMethods.find(_.getName == name).get.invoke(ref)
    def setByName(name: String, value: Any): Unit = ref.getClass.getMethods.find(_.getName == name + "_$eq").get.invoke(ref, value.asInstanceOf[AnyRef])
  }

  def configure(parameters: Map[String, Any]) = {
    parameters.foreach { case (k, v) => this.setByName(k, v) }
  }

}