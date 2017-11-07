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
  
//  var file : File 
//  var target : String 
//  
  def run(body: MultipartFormData[Files.TemporaryFile]) = {
    upload(body)
  }
  
    /**
   * Upload file to selected directory
   */
  def upload(body: MultipartFormData[Files.TemporaryFile]) {
    val dirname: String = body.asFormUrlEncoded.get("dir").get(0)
    body.file(taskName).map { taskFile =>
      val filename = taskFile.filename;
      val contentType = taskFile.contentType;
      if (filename.equals(""))
        // flashing does not work on Chrome or Safari
        println("Missing File")
      else {
        if (dirname.equals(""))
          // no directory selected, use default directory
          taskFile.ref.moveTo(Paths.get(s"tmp/$filename"), replace = true);
        else if (java.nio.file.Files.exists(Paths.get(dirname)))
          // validate path
          taskFile.ref.moveTo(new File(s"$dirname/$filename"), replace = true);
      }
    }
  }
}