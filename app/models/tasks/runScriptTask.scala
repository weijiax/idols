package models.tasks

import play.api.mvc._
import sys.process._
import models.tasks.helperFunctions._
import java.io._
import scala.collection.Seq
import play.api.libs.json._

class runScriptTask(json: JsValue) extends Task(json) {
  //name of this task, example: preprocessing, data analysis, postprocessing
  val task_name = (json \ "task_name").as[String].replace("\"", "")
  // type of this task, example: fileUpload
  val task_type = (json \ "task_type").as[String].replace("\"", "")
  val task_description = (json \ "task_description").as[String].replace("\"", "")
  val access_level = if ((json \ "access_level").as[String].replace("\"", "").equals("Admin")) models.auth.Roles.AdminRole else models.auth.Roles.UserRole

  //  var file : File
  //  var target : String
  //
  def run(body: AnyContent): String = {
    textEditor(body)
  }

  /**
   * check Cluster info, node list, etc.
   */
  def textEditor(body: AnyContent): String = {
    var feedback = ""

    val userInput = body.asFormUrlEncoded
    val file_path = userInput.get("file_path")(0)

    val button = userInput.get("action")(0)

    println(button)

    if (button == "edit") {

      if (new java.io.File(file_path).exists) {
        if (new java.io.File(file_path).isFile()) {

          val command = "cat " + " " + file_path
          val res = Process(Seq("bash", "-c", command)).!!
          feedback = res

          println(feedback)
        }
        if (new java.io.File(file_path).isDirectory()) {
          feedback = "Failed: It is a directory. "
        }

      } else {
        feedback = "Failed: Path does not exist. "
      }
    }

    if (button == "save") {
      val text_area = userInput.get("text_area")(0)
      println(text_area)

      val new_file_name = save(text_area, file_path)

      feedback = "Saved as " + new_file_name
    }
    if (button == "run") {
      val text_area = userInput.get("text_area")(0)
      println(text_area)

      val new_file_name = save(text_area, file_path)

      val command = "source " + " " + new_file_name
      val res = Process(Seq("bash", "-c", command)).!

      res match {
        case 0 => { feedback = "Run successfully" }
        case _ => { feedback = "Failed: somethime wrong with run" }
      }

    }
    feedback
  }

  // save edited text from file_path to a new file "edit_" + file_path
  // return new file absolute path
  def save(text: String, file_path: String): String = {
    val new_file_name = file_path.reverse.replaceFirst("/", "edit_".reverse + "/").reverse
    println(new_file_name)
    val file = new File(new_file_name)
    val bw = new BufferedWriter(new FileWriter(file))
    bw.write(text)
    bw.close()

    new_file_name

  }

}