package models

import play.api.mvc._
import models.helperFunctions._


class checkClusterTask(name: String, tType: String) extends Task(name, tType){
   //name of this task, example: preprocessing, data analysis, postprocessing
  val taskName = name
  // type of this task, example: fileUpload
  val taskType = tType
  
//  var file : File 
//  var target : String 
//  
  def run(body: AnyContent) : String= {
    checkCluster(body)
  }
  
    /**
   * check Cluster info, node list, etc.
   */
  def checkCluster(body: AnyContent) : String = {
    val cluster  = new HadoopCluster(3,3,"Hadooop")
    val reservationName = body.asFormUrlEncoded.get("reservation")(0)
    val res_info = cluster.getReservationInfo(reservationName)
    val node_list = cluster.getHadoopClusterNodeList(reservationName)
    val break = "<p></p>"
    arrayToHtml("Reservation information:", res_info, "div") + break + arrayToHtml("Node list:", node_list, "div")
    }
  
  // description, 
  //tag is div, p, or others to form <div></div>
  
    
}