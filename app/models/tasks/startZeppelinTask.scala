package models.tasks

import play.api.mvc._
import models.tasks.helperFunctions._
import scala.sys.process._
import scala.collection.Seq
import play.api.libs.json._

class startZeppelinTask(json: JsValue) extends Task(json) {
  //name of this task, example: preprocessing, data analysis, postprocessing
  val task_name = (json \ "task_name").as[String].replace("\"", "")
  // type of this task, example: fileUpload
  val task_type = (json \ "task_type").as[String].replace("\"", "")
  val task_description = (json \ "task_description").as[String].replace("\"", "")
  val access_level = if ((json \ "access_level").as[String].replace("\"", "").equals("Admin")) models.auth.Roles.AdminRole else models.auth.Roles.UserRole

  //  var file : File
  //  var target : String
  //
  def run(body: AnyContent): String = {
    startZeppelin(body)
  }

  /**
   * check Cluster info, node list, etc.
   */
  def startZeppelin(body: AnyContent): String = {
    var feedback = ""

    val reservationName = body.asFormUrlEncoded.get("reservation")(0)
    //val reservationName ="hadoop+Idols+2431"

    val project = reservationName.split("\\+")(1)

    val zeppelin_script_path = " /data/apps/zeppelin_user/job.zeppelin"
    val command = "ssh -n login1  sbatch --reservation=" + reservationName + " -A " + project + " " + zeppelin_script_path
    val res = Process(Seq("bash", "-c", command)).!

    // get zeppelin ui url from zepplein.out file
    val command_1 = "cd $HOME && grep 'Application UI' zeppelin.out"

    res match {
      case 0 => { Thread.sleep(10000); feedback = Process(Seq("bash", "-c", command_1)).!!.split("\n")(0).split(" ").last }
      case _ => { feedback = "Failed: wrong reservation name" }
    }

    feedback

  }

  // description,
  //tag is div, p, or others to form <div></div>

}