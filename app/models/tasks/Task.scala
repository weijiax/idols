package models.tasks

import play.api.mvc._
import play.api.libs.json._
import java.nio.file._

abstract class Task(json: JsValue) {
  //name of this task, example: preprocessing, data analysis, postprocessing
  val task_name = (json \ "task_name").as[String].replace("\"", "")
  // type of this task, example: fileUpload
  val task_type = (json \ "task_type").as[String].replace("\"", "")
  // access level of this task
  val access_level = if ((json \ "access_level").as[String].replace("\"", "").equals("Admin")) models.auth.Roles.AdminRole else models.auth.Roles.UserRole

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
  def run(body: AnyContent): String

  implicit def reflector(ref: AnyRef) = new {
    def getByName(name: String): Any = ref.getClass.getMethods.find(_.getName == name).get.invoke(ref)
    def setByName(name: String, value: Any): Unit = ref.getClass.getMethods.find(_.getName == name + "_$eq").get.invoke(ref, value.asInstanceOf[AnyRef])
  }

  def configure(parameters: Map[String, Any]) = {
    parameters.foreach { case (k, v) => this.setByName(k, v) }
  }

}