package models.tasks

import play.api.mvc._
import sys.process._
import models.tasks.helperFunctions._
import scala.collection.Seq
import play.api.libs.json._
import java.nio.file.Paths

class showResultTask(json: JsValue) extends Task(json) {
  //  var file : File
  //  var target : String
  //

  val output_path_string = (json \ "file_path").as[String].replace("\"", "")
  // interpret ~/, $USER, $HOME, $WORK
  val output_path = Process(Seq("bash", "-c", "echo " + output_path_string)).!!.split("\n")(0)

  def run(body: AnyContent): String = {
    showOutput(body)
  }

  /**
   * check Cluster info, node list, etc.
   */
  def showOutput(body: AnyContent): String = {
    var feedback = ""

    val userInput = body.asFormUrlEncoded
    val output_path_string = userInput.get("output_path")(0)

    // interpret ~/, $USER, $HOME, $WORK
    val output_path = Process(Seq("bash", "-c", "echo " + output_path_string)).!!.split("\n")(0)

    println(output_path)

    if (task_name == "Read File in HDFS") {
      val top_n = userInput.get("top_n")(0)

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
      val top_n = userInput.get("top_n")(0)

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

    if (task_name == "Show Image") {
      val command = "cp " + output_path + " ./public/images/"
      val test = Process(Seq("bash", "-c", command)).!
      val p = Paths.get(output_path);
      val file_name = p.getFileName

      println(file_name)

      Thread.sleep(800)

      if (test == 0) { // if path exist
        feedback = "image_show:" + file_name

      } else { // if path not exist
        feedback = "Failed: path does not exist. "
      }
      //feedback = "image_show"
    }

    feedback

  }

}