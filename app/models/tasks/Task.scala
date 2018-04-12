package models.tasks

import play.api.mvc._
import play.api.libs.json._
import java.nio.file._

abstract class Task(json: JsValue) {
  //  val json = json;

  //name of this task, example: preprocessing, data analysis, postprocessing
  val task_name = (json \ "task_name").as[String].replace("\"", "")
  // type of this task, example: fileUpload
  val task_type = (json \ "task_type").as[String].replace("\"", "")

  //  val task_description = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get((json \ "task_description").as[String].replace("\"", ""))))
  val access_level = if ((json \ "access_level").as[String].replace("\"", "").equals("Admin")) models.auth.Roles.AdminRole else models.auth.Roles.UserRole

  //  val id: Int

  def get_json(): JsValue = {
    return json
  }

  def get_description(): String = {
    val format = (json \ "task_description" \ "format").as[String].replace("\"", "")
    val content = (json \ "task_description" \ "content").as[String].replace("\"", "")
    val file = (json \ "task_description" \ "file").as[String].replace("\"", "")

    if (content == "") { // read from file
      val path = Paths.get(file)
      if (Files.exists(path)) {
        // Conver the content of file to HTML
        return utils.Reader.toHTML(format, new String(java.nio.file.Files.readAllBytes(path)))
      } else {
        // No such file, assume dexcription is plain text
        return "No such file"
      }
    } else {
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