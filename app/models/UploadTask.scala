package models

import play.api.libs.Files
import play.api.libs.json._
import play.api.mvc._
import java.io.File
import java.nio.file.Paths

class UploadTask(name: String, tType: String) extends Task(name, tType){
   //name of this task, example: preprocessing, data analysis, postprocessing
  val taskName = name
  // type of this task, example: fileUpload
  val taskType = tType
  
  /**
   * Run this task
   * @param body: message requested from user
   * @return feedback to user
   */
  def run(body: AnyContent): Array[String] = {
    upload(body.asMultipartFormData.get)
  }

  /**
   * Upload file to selected directory
   * @param body: information requested from user
   * @return string indicating whether the task was successful or an error has occurred
   */
  def upload(body: MultipartFormData[Files.TemporaryFile]): Array[String] = {
    var feedback: Array[String] = new Array[String](1)
    val dirname: String = body.asFormUrlEncoded.get("dir").get(0)
    // check if a file has been selected
    if (body.file(taskName).equals(None)) {
      feedback(0) = "Error: Missing File"
      return feedback
    } else {
      body.file(taskName).map { taskFile =>
        val filename = taskFile.filename;
        val contentType = taskFile.contentType;
        if (dirname.equals(""))
          // no directory selected, use default directory
          taskFile.ref.moveTo(Paths.get(s"tmp/$filename"), replace = true);
        else if (java.nio.file.Files.exists(Paths.get(dirname)))
          // validate path
          taskFile.ref.moveTo(new File(s"$dirname/$filename"), replace = true);
        feedback(0) = "Success: File Uploaded"
        return feedback
      }
    }
     feedback(0) = "Unexpected error"
    return feedback
  }
}