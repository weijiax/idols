package models.tasks

import play.api.libs.Files
import play.api.libs.json._
import play.api.mvc._
//import play.api.libs.json.JsValue.jsValueToJsLookup
//import models.tasks.Task

import java.io.File
import java.nio.file._
import sys.process._

class UploadTask(json: JsValue) extends Task(json) {
  val root_string = (json \ "value1").as[String].replace("\"", "")
  // interpret ~/, $USER, $HOME, $WORK
  val root = Process(Seq("bash", "-c", "echo " + root_string)).!!.split("\n")(0)

  /**
   * Run this task
   * @param body: message requested from user
   * @return feedback to user
   */
  def run(body: AnyContent, session: Int): String = {
    upload(body.asMultipartFormData.get)
  }

  /**
   * Upload file to selected directory
   * @param body: information requested from user
   * @return string indicating whether the task was successful or an error has occurred
   */
  def upload(body: MultipartFormData[Files.TemporaryFile]): String = {
    var feedback: String = ""
    val dirname_string: String = body.asFormUrlEncoded.get("dir").get(0)
    // interpret ~/, $USER, $HOME, $WORK
    val dirname = Process(Seq("bash", "-c", "echo " + dirname_string)).!!.split("\n")(0)

    // check if a file has been selected
    if (body.file(task_name).equals(None)) {
      feedback = "Failed: Missing File"
      return feedback
    } else {
      body.file(task_name).map { taskFile =>
        val filename = taskFile.filename;
        val contentType = taskFile.contentType;
        if (dirname.equals("")) {
          feedback = "Failed: Missing Directory"
          return feedback
        } else if (java.nio.file.Files.exists(Paths.get(dirname))) {
          // validate path
          taskFile.ref.moveTo(new File(s"$dirname/$filename"), replace = true);
          feedback = "Success: File Uploaded"
          return feedback
        } else {
          feedback = "Failed: Directory does not exist"
          return feedback
        }
      }
    }
    feedback = "Unexpected error"
    return feedback
  }
}