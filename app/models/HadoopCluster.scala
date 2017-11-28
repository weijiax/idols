package models

/**
 * Presentation object used for displaying data in a template.
 *
 * Note that it's a good practice to keep the presentation DTO,
 * which are used for reads, distinct from the form processing DTO,
 * which are used for writes.
 */
import play.api.libs.Files
import play.api.mvc._
import scala.sys.process._
import Array._


 class HadoopCluster(numNode: Int, numDays:Int, cType: String) extends Cluster(numNode,numDays,cType){
  //number of node for the cluster
  val numberNode = numNode
  
  // number of days for the cluster
  val numberDays = numDays
  
  // type of this task, example: fileUpload
  val clusterType = cType
  
//  val id: Int
  
  /**
   * Run this task
   * @param body: message requested from user
   * @return feedback to user
   */
  def create() = {    
    createHadoopCluster()
  }
  
  def createHadoopCluster() = {
    // run bash script to create hadoop reservation
  }



  override def configure(parameters: Map[String, Any]) = {
      configureHadoopCluster(parameters)
    //parameters.foreach { case (k, v) => this.setByName(k, v) }
  }
  
  def configureHadoopCluster(parameters: Map[String, Any]){
    // configure 
    
  }
  
  def getNodeList(reservationName: String) : Array[String] = {
    getHadoopClusterNodeList(reservationName)    
  }
  
  def getReservationInfo(reservationName:String = "hadoop+Idols+2431"):Array[String] = {
    val resName = reservationName
    val command = "scontrol show reservation |grep " + resName
    
    val res_info = Process(Seq("bash","-c", command)).!!.split("\n")
    //laptop
    //val res_info = Array("ReservationName=hadoop+Idols+2431 StartTime=2017-11-22T11:35:02 EndTime=2017-12-02T11:35:02 Duration=10-00:00:00")
    
    res_info    
  }
  
  def getHadoopClusterNodeList(reservationName:String = "hadoop+Idols+2431"):Array[String] = {
    val resName = reservationName
    val command_1 = "tmp=`sinfo -T|grep "+ resName + " |awk '{print $6}'` && scontrol show hostname $tmp"
    
    val nodelist = Process(Seq("bash","-c", command_1)).!!.split("\n")
    //laptop
    //val nodelist = Array("c252-101","c252-102","c252-103", "c252-104")
    
    nodelist    
  }
  

}