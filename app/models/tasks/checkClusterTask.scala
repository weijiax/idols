package models.tasks

import play.api.mvc._
import models.tasks.helperFunctions._
import models.HadoopCluster
import play.api.libs.json._

class checkClusterTask(json: JsValue) extends Task(json) {
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
    checkCluster(body)
  }

  /**
   * check Cluster info, node list, etc.
   */
  def checkCluster(body: AnyContent): String = {
    var feedback = ""

    val cluster = new HadoopCluster(3, 3, "Hadooop")
    val reservationName = body.asFormUrlEncoded.get("reservation")(0)

    try {
      val res_info = cluster.getReservationInfo(reservationName)
      val node_list = cluster.getHadoopClusterNodeList(reservationName)
      val break = "<p></p>"
      feedback = arrayToHtml("Reservation information:", res_info, "div") + break + arrayToHtml("Node list:", node_list, "div")
    } catch {
      case e: Exception => {
        feedback = "Failed: wrong reservation name"
      }
    }

    feedback

  }

  // description,
  //tag is div, p, or others to form <div></div>

}