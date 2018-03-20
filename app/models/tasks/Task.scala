package models.tasks

import play.api.mvc._
import play.api.libs.json._

abstract class Task(json: JsValue) {
//  val json = json;
  
  //name of this task, example: preprocessing, data analysis, postprocessing
  val task_name: String
  // type of this task, example: fileUpload
  val task_type: String
  val task_description: String
  val access_level: models.auth.Roles.Role

  //  val id: Int
  
  def get_json(): JsValue = {
    return json
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