package models

import play.api.libs.Files
import play.api.mvc._


abstract class Task(name: String, tType: String) {
  //name of this task, example: preprocessing, data analysis, postprocessing
  val taskName: String
  // type of this task, example: fileUpload
  val taskType: String
  
//  val id: Int
  
  /**
   * Run this task
   * @param body: message requested from user
   * @return feedback to user
   */
  def run(body: AnyContent): Array[String]


  implicit def reflector(ref: AnyRef) = new {
    def getByName(name: String): Any = ref.getClass.getMethods.find(_.getName == name).get.invoke(ref)
    def setByName(name: String, value: Any): Unit = ref.getClass.getMethods.find(_.getName == name + "_$eq").get.invoke(ref, value.asInstanceOf[AnyRef])
  }
 
  def configure(parameters: Map[String, Any]) = {
    parameters.foreach { case (k, v) => this.setByName(k, v) }
  }

}