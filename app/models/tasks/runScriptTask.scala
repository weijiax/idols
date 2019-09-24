package models.tasks

import javax.inject._
import play.api.mvc._
import sys.process._
import models.tasks.helperFunctions._
import java.io._
import scala.collection.Seq
import play.api.libs.json._
import utils._
import play.api._
import play.api.i18n.I18nSupport
import com.typesafe.config._

//@Singleton
class runScriptTask(json: JsValue) extends Task(json) with ScriptTrait {
  //  var file : File
  //  var target : String
  //

  //println(conf.getString("idols.mode"))

  val path = (json \ "file_path").as[String].replace("\"", "")

  def run(body: AnyContent, session: Int): String = {
    textEditor(body)
  }

  /**
   * check Cluster info, node list, etc.
   */
  def textEditor(body: AnyContent): String = {
    var feedback = ""

    val conf = ConfigFactory.load()
    var idoles_mode: String = ""
    idoles_mode = conf.getString("idols.mode")
    println("MODE = " + idoles_mode)

    val userInput = body.asFormUrlEncoded
    val file_path_string = userInput.get("file_path")(0)

    // interpret ~/, $USER, $HOME, $WORK
    val file_path = Process(Seq("bash", "-c", "echo " + file_path_string)).!!.split("\n")(0)

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
      //println(text_area)

      try {
        val new_file_name = save(text_area, file_path)
        feedback = "Saved as " + new_file_name
      } catch {
        case e: Exception => {
          feedback = "Failed: " + e.toString()
        }
      }

    }
    if (button == "run") {
      val text_area = userInput.get("text_area")(0)
      //println("******************************************")

      try {
        val new_file_name = save(text_area, file_path)

        val command = "source " + " " + new_file_name

        // clear before append
        stdout.clear(); stderr.clear()

        val status = Process(Seq("bash", "-c", command)).!(ProcessLogger(stdout.append(_), stderr.append(_)))
        println("status= " + status)
        //println("stattus_1= " + Process(Seq("bash", "-c", command)).!)
        println("stdout=" + stdout)
        println("stderr=" + stderr)

        status match {
          case 0 => { feedback = "Run successfully" + arrayToHtml("standard output: ", stdout.toArray) }
          case _ => { feedback = "Failed: " + arrayToHtml("standard error: ", stderr.toArray) }
        }
      } catch {
        case e: Exception => {
          feedback = "Failed: " + e.toString()
          println(e)
        }
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