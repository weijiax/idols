package models.tasks

import play.api.mvc._
import sys.process._
import models.tasks.helperFunctions._
import scala.collection.Seq
import play.api.libs.json._

class showResultTask(json: JsValue) extends Task(json) {
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
    showOutput(body)
  }

  /**
   * check Cluster info, node list, etc.
   */
  def showOutput(body: AnyContent): String = {
    var feedback = ""

    val userInput = body.asFormUrlEncoded
    val output_path = userInput.get("output_path")(0)
    val top_n = userInput.get("top_n")(0)

    if (task_name == "Read File in HDFS") {
      // test if HDFS path exists
      val command_0 = "hdfs dfs -test -d " + output_path
      // test = 0 exist, test=1 not exist
      val test = Process(Seq("bash", "-c", command_0)).!

      if (test == 0) { // if path exist
        //laptop
        //val command = "head -n " + top_n + " " + output_path
        val command = "hadoop fs -cat " + output_path + "/part-r-00000 | head -n " + top_n

        //need error handler here...
        val res = Process(Seq("bash", "-c", command)).!!.split("\n")

        feedback = arrayToHtml("Results:", res, "div")

      } else { // if path not exist
        feedback = "Failed: HDFS path does not exist. "
      }
    }

    if (task_name == "Read File in Lustre") {
      if (new java.io.File(output_path).exists) {
        if (new java.io.File(output_path).isFile()) {
          val command = "head -n " + top_n + " " + output_path
          val res = Process(Seq("bash", "-c", command)).!!.split("\n")
          feedback = arrayToHtml("Results:", res, "div")
          println(res(0))
        }
        if (new java.io.File(output_path).isDirectory()) {
          feedback = "Failed: It is a directory. "
        }

      } else {
        feedback = "Failed: Path does not exist. "
      }
    }

    feedback

  }

}