package models.tasks

import play.api.mvc._
import sys.process._
import models.tasks.helperFunctions._
import java.io._
import scala.collection.Seq
import play.api.libs.json._
import utils._

class RunInputScriptTask(json: JsValue) extends Task(json) with ScriptTrait {
  val inputs = (json \ "inputs").get.toString()

  val path = (json \ "executable_path").as[String].replace("\"", "")
  val num_inputs = (json \ "inputs").get.as[JsObject].value.size

  def run(body: AnyContent, session: Int): String = {
    textEditor(body, session)
  }

  /**
   * check Cluster info, node list, etc.
   */
  def textEditor(body: AnyContent, session: Int): String = {
    var feedback = ""

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

          // replace input values
          var i = 1
          for (i <- 1 until num_inputs + 1) {
            feedback = feedback.replace("$" + i, userInput.get("$" + i)(0))
          }

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
      if (new java.io.File(file_path).exists) {
        if (new java.io.File(file_path).isFile()) {

          val command = "cat " + " " + file_path
          val res = Process(Seq("bash", "-c", command)).!!
          feedback = res

          // replace input values
          var i = 1
          for (i <- 1 until num_inputs + 1) {
            feedback = feedback.replace("$" + i, userInput.get("$" + i)(0))
          }

          println(feedback)
        }
        if (new java.io.File(file_path).isDirectory()) {
          feedback = "Failed: It is a directory. "
        }

      } else {
        feedback = "Failed: Path does not exist. "
      }
      utils.ScriptScheduler.addSession(session)

      while (!utils.ScriptScheduler.canRunSession(session)) {
        Thread.sleep(2000)
      }

      print("starting session" + session)

      val command = "cat " + " " + file_path
      val res = Process(Seq("bash", "-c", command)).!!
      var text_area = res

      // replace input values
      var i = 1
      for (i <- 1 until num_inputs + 1) {
        text_area = text_area.replace("$" + i, userInput.get("$" + i)(0))
      }

      //println("******************************************")

      // check idols run on local laptop or cluster,
      // hostname_last=='local' then laptop, otherwise cluster (i.e. hostname_last=='edu' )
      val hostname_last = getHostNameLast
      println(hostname_last)

      try {
        val new_file_name = save(text_area, file_path)

        val session_command = "if [ ! -d '" + session + "' ]; then mkdir ./" + session + "; fi"
        val command = "source " + " " + new_file_name

        // clear before append
        stdout.clear(); stderr.clear()

        var status = 0
        if (hostname_last == "local") {
          status = Process(Seq("bash", "-c", command)).!(ProcessLogger(stdout.append(_), stderr.append(_)))
        } else {
          status = Process(Seq("bash", "-c", command)).!(ProcessLogger(stdout.append(_), stderr.append(_)))
        }

        println("status= " + status)
        //println("stattus_1= " + Process(Seq("bash", "-c", command)).!)
        println("stdout=" + stdout)
        println("stderr=" + stderr)

        utils.ScriptScheduler.finishedSession(session)
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