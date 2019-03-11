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

  val output_path = (json \ "file_path").as[String].replace("\"", "")
  val text_or_image = (json \ "text_or_image").as[String].replace("\"", "").toLowerCase()
  val hadoop_file_system_default = (json \ "hadoop_file_system").as[String].replace("\"", "").toLowerCase()

  // for selection box
  var hadoop_file_system_second = "yes"
  if (hadoop_file_system_default == "yes") {
    hadoop_file_system_second = "no"
  }
  // handle error from workflow json file, if hadoop_file_system_default is neither "yes" or "no"
  var hadoop_file_system_err = false
  if (hadoop_file_system_default != "yes" && hadoop_file_system_default!= "no") {
    hadoop_file_system_err = true
  }

  def run(body: AnyContent): String = {
    showOutput(body)
  }

  /**
   * check Cluster info, node list, etc.
   */
  def showOutput(body: AnyContent): String = {
    var feedback = ""

    val userInput = body.asFormUrlEncoded
    val hadoop_file_system_input = userInput.get("file_system")(0).toLowerCase()
    val output_path_string = userInput.get("output_path")(0)

    println(hadoop_file_system_input)

    // interpret ~/, $USER, $HOME, $WORK
    val output_path = Process(Seq("bash", "-c", "echo " + output_path_string)).!!.split("\n")(0)

    println(output_path)

    if (text_or_image == "text" && hadoop_file_system_input == "yes") {
      val top_n = userInput.get("top_n")(0)

      // test if HDFS path exists
      val command_0 = "hdfs dfs -test -d " + output_path
      // test = 0 exist, test=1 not exist
      val test = Process(Seq("bash", "-c", command_0)).!

      if (test == 0) { // if path exist
        //laptop
        //val command = "head -n " + top_n + " " + output_path
        val command = "hadoop fs -cat " + output_path + "/* | head -n " + top_n

        //need error handler here...
        val res = Process(Seq("bash", "-c", command)).!!.split("\n")

        feedback = arrayToHtml("Results:", res, "div")

      } else { // if path not exist
        feedback = "Failed: HDFS path does not exist. "
      }
    }

    if (text_or_image == "text" && hadoop_file_system_input == "no") {
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

    if (text_or_image == "image") {
      //println("**************************")

      val file_name = "tmp_" + scala.util.Random.nextInt(100) + ".png"
      val public_dir = "./public/images/"
      val command = "rm -f " + public_dir + "tmp_* ; " + " cp " + output_path + " " + public_dir + file_name
      val test = Process(Seq("bash", "-c", command)).!
      //val p = Paths.get(output_path);
      // val file_name = p.getFileName

      println(file_name)

      Thread.sleep(600)

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