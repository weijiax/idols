package models.tasks

import play.api.mvc._
import models.tasks.helperFunctions._
import models.HadoopCluster
import play.api.libs.json._

class checkClusterTask(json: JsValue) extends Task(json) {
  //  var file : File
  //  var target : String
  //

  val reservationName = (json \ "reservationName").as[String].replace("\"", "")

  def run(body: AnyContent, session: Int): String = {
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