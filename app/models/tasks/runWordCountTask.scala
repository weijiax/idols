package models.tasks

import play.api.mvc._
import sys.process._
import scala.collection.Seq
import play.api.libs.json._

class runWordCountTask(json: JsValue) extends Task(json) {
  //  var file : File
  //  var target : String
  //
  def run(body: AnyContent, session: Int): String = {
    runWordCount(body)
  }

  /**
   * check Cluster info, node list, etc.
   */
  def runWordCount(body: AnyContent): String = {
    var feedback = ""

    val userInput = body.asFormUrlEncoded
    val script_path = userInput.get("script_path")(0)

    if (!new java.io.File(script_path).exists) {
      feedback = "Failed: script path does not exists. "
    }

    val input_file_path = userInput.get("input_file_path")(0)

    if (!new java.io.File(input_file_path).exists) {
      if (!new java.io.File(script_path).exists) {
        feedback = feedback + "Failed: input file path does not exists. "
      } else {
        feedback = "Failed: input file path does not exists. "
      }
    }

    val output_path = userInput.get("output_path")(0)

    val command = "source " + script_path + " " + input_file_path + " " + output_path + " &> /dev/null  & echo $!"

    if (new java.io.File(script_path).exists && new java.io.File(input_file_path).exists) {
      feedback = "Job submitted with process ID: " + Process(Seq("bash", "-c", command)).!!.split("\n")(0)
    }

    feedback

    //    res match {
    //      case 0 => "Job finished"
    //      case _ => "Something went wrong"
    //    }
  }

}