package models

import play.api.libs.Files
import play.api.libs.json._
import play.api.mvc._
import java.io.File
import java.nio.file.Paths

case class Task(name: String, tType: String) {
  //name of this task, example: preprocessing, data analysis, postprocessing
  val taskName = name
  // type of this task, example: fileUpload
  val taskType = tType
  
//  val id: Int

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

  implicit def reflector(ref: AnyRef) = new {
    def getByName(name: String): Any = ref.getClass.getMethods.find(_.getName == name).get.invoke(ref)
    def setByName(name: String, value: Any): Unit = ref.getClass.getMethods.find(_.getName == name + "_$eq").get.invoke(ref, value.asInstanceOf[AnyRef])
  }
  def run() = {}
  def configure(parameters: Map[String, Any]) = {
    parameters.foreach { case (k, v) => this.setByName(k, v) }
  }

}