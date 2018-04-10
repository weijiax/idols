package models.tasks

import play.api.mvc._
import play.api.libs.json._

abstract class Task(json: JsValue) {
  //  val json = json;

  //name of this task, example: preprocessing, data analysis, postprocessing
  val task_name = (json \ "task_name").as[String].replace("\"", "")
  // type of this task, example: fileUpload
  val task_type = (json \ "task_type").as[String].replace("\"", "")
  val task_description = (json \ "task_description").as[String].replace("\"", "")

  //  val task_description = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get((json \ "task_description").as[String].replace("\"", ""))))
  val access_level = if ((json \ "access_level").as[String].replace("\"", "").equals("Admin")) models.auth.Roles.AdminRole else models.auth.Roles.UserRole

  //  val id: Int

  def get_json(): JsValue = {
    return json
  }
  
  def get_description(): String = {
    val path = java.nio.file.Paths.get((json \ "task_description").as[String].replace("\"", ""))
    val mime = java.nio.file.Files.probeContentType(path)
    mime match {
      case "text/markdown"
    }
    val task_description = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get((json \ "task_description").as[String].replace("\"", ""))))

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