package models.tasks

import play.api.libs.Files
import play.api.libs.json._
import play.api.mvc._
//import play.api.libs.json.JsValue.jsValueToJsLookup
import models.tasks.Task
import java.io.File
import java.nio.file._

class UploadTask(json: JsValue) extends Task(json) {
  //  //name of this task, example: preprocessing, data analysis, postprocessing
  //  val task_name = (json \ "task_name").as[String].replace("\"", "")
  //  // type of this task, example: fileUpload
  //  val task_type = (json \ "task_type").as[String].replace("\"", "")
  //  val task_description = new String(java.nio.file.Files.readAllBytes(Paths.get((json \ "task_description").as[String].replace("\"", ""))))
  //  val access_level = if ((json \ "access_level").as[String].replace("\"", "").equals("Admin")) models.auth.Roles.AdminRole else models.auth.Roles.UserRole
  val root = (json \ "value1").as[String].replace("\"", "")

  /**
   * Run this task
   * @param body: message requested from user
   * @return feedback to user
   */
  def run(body: AnyContent): String = {
    upload(body.asMultipartFormData.get)
  }

  /**
   * Upload file to selected directory
   * @param body: information requested from user
   * @return string indicating whether the task was successful or an error has occurred
   */
  def upload(body: MultipartFormData[Files.TemporaryFile]): String = {
    var feedback: String = ""
    val dirname: String = body.asFormUrlEncoded.get("dir").get(0)
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