package models.tasks
import play.api.mvc._
import sys.process._
import models.tasks.helperFunctions._
import scala.collection.Seq
import play.api.libs.json._
import play.api.data._
import play.api.i18n._
import java.nio.file.Paths
import java.io.File
import scala.io.Source
import play.api.libs.Files

import javax.inject.Inject

class showResultTask @Inject() (json: JsValue) extends Task(json) {
  //  var file : File
  //  var target : String
  //

  val output_path = (json \ "file_path").as[String].replace("\"", "")

  val show_type = (json \ "file_type").as[String].replace("\"", "")
  //  if (!(json \ "show_type").asOpt[String].isEmpty) {
  //    show_type = (json \ "file_type").as[String].replace("\"", "")
  //  }

  def run(body: AnyContent, session: Int): String = {
    showOutput(body)
  }

  //  def getVideo(path: String, name: String) = Action {
  //    println("get file: " + name + " at Path: " + path)
  //    Ok.sendFile(new File(path + name.replace("%20", " ")))
  //  }

  /**
   * check Cluster info, node list, etc.
   */
  def showOutput(body: AnyContent): String = {
    var feedback = ""

    val userInput = body.asFormUrlEncoded

    val hadoop_file_system_input = userInput.get("file_system")(0).toLowerCase()
    val output_path_image_input = userInput.get("output_path")(0)
    val output_path_text_input = userInput.get("output_path")(0)
    val output_path_json_input = userInput.get("output_path")(0)
    val output_path_audio_input = userInput.get("output_path")(0)

    //    if ((json \ "show_type").asOpt[String].isEmpty) {
    //      show_type = userInput.get("action")(0)
    //    }

    println(hadoop_file_system_input)

    // interpret ~/, $USER, $HOME, $WORK
    val output_path_image = Process(Seq("bash", "-c", "echo " + output_path_image_input)).!!.split("\n")(0).replace(" ", "\\ ")
    println(output_path_image)
    val output_path_text = Process(Seq("bash", "-c", "echo " + output_path_text_input)).!!.split("\n")(0).replace(" ", "\\ ")
    println(output_path_text)
    val output_path_json = Process(Seq("bash", "-c", "echo " + output_path_json_input)).!!.split("\n")(0).replace(" ", "\\ ")
    println(output_path_json)

    //val button = userInput.get("action")(0)
    //println(button)

    if (show_type == "show_text") {
      if (hadoop_file_system_input == "yes") {

        //val top_n = userInput.get("top_n")(0)
        val top_n = 10

        // test if HDFS path exists
        val command_0 = "hdfs dfs -test -d " + output_path_text
        // test = 0 exist, test=1 not exist
        val test = Process(Seq("bash", "-c", command_0)).!

        if (test == 0) { // if path exist
          //laptop
          //val command = "head -n " + top_n + " " + output_path
          val command = "hadoop fs -cat " + output_path_text + "/* | head -n " + top_n

          //need error handler here...
          val res = Process(Seq("bash", "-c", command)).!!.split("\n")

          feedback = arrayToHtml("Results:", res, "div")

        } else { // if path not exist
          feedback = "Failed: HDFS path does not exist. "
        }
      }

      if (hadoop_file_system_input == "no") {
        val top_n = 100

        if (new java.io.File(output_path_text).exists) {
          if (new java.io.File(output_path_text).isFile()) {
            val command = "head -n " + top_n + " " + output_path_text
            val res = Process(Seq("bash", "-c", command)).!!.split("\n")
            feedback = arrayToHtml("Results:", res, "div")
            println(res(0))
          }
          if (new java.io.File(output_path_text).isDirectory()) {
            feedback = "Failed: It is a directory. "
          }

        } else {
          feedback = "Failed: Path does not exist. "
        }
      }

    } else if (show_type == "show_image") {

      val file_name = "tmp_" + scala.util.Random.nextInt(100) + ".png"
      val public_dir = "./public/images/"
      val command = "rm -f " + public_dir + "tmp_* ; " + " cp " + output_path_image + " " + public_dir + file_name

      val test = Process(Seq("bash", "-c", command)).!
      //val p = Paths.get(output_path);
      // val file_name = p.getFileName
      println(command)

      println(test)

      Thread.sleep(600)

      if (test == 0) { // if path exist
        feedback = "image_show:" + file_name

      } else { // if path not exist
        feedback = "Failed: path does not exist. "
      }
    } else if (show_type == "show_JSON") {
      //When the target of showResultTask is a JSON file
      if (new java.io.File(output_path_json).exists) {
        if (new java.io.File(output_path_json).isFile()) {
          var json_result = ""
          val jsonString = Source.fromFile(output_path_json).getLines.mkString
          //val resultString = jsonString.substring(jsonString.indexOf('[') + 1, jsonString.lastIndexOf(']'))
          feedback = "JSON_show:" + jsonString

          //print out feedback
          //println("FEEDBACK:: " + feedback)
        }
        if (new java.io.File(output_path_json).isDirectory()) {
          feedback = "Failed: It is a directory. "
        }

      } else {
        feedback = "Failed: Path does not exist. "
      }
    } else if (show_type == "show_audio") {
      //Ok.sendFile(new File(output_path_audio_input))
    } else { // go into here when download button clicked
      feedback = "Sucessfull downloaded"
    }

    feedback

  }

}