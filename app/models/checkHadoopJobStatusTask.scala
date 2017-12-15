package models

import play.api.mvc._
import models.helperFunctions._


class checkHadoopJobStatusTask(name: String, tType: String) extends Task(name, tType){
   //name of this task, example: preprocessing, data analysis, postprocessing
  val taskName = name
  // type of this task, example: fileUpload
  val taskType = tType
  
  
//  var file : File 
//  var target : String 
//  
  def run(body: AnyContent) : String= {
    checkHadoopJob(body)
  }
  
    /**
   * check Cluster info, node list, etc.
   */
  def checkHadoopJob(body: AnyContent) : String = {
    var feedback = ""
    
    val reservationName = body.asFormUrlEncoded.get("reservation")(0)
    val job_status = body.asFormUrlEncoded.get("job_status")(0)
    
    println(reservationName)
    println(job_status)
    
    
    val cluster  = new HadoopCluster(3,3,"Hadooop")
    // val res_info = cluster.getReservationInfo(reservationName)
    
    
    try {
      
          val res_info = cluster.getReservationInfo(reservationName)
          
          val node_list = cluster.getHadoopClusterNodeList(reservationName)
    
          val apps = listHapdoopJob(job_status)
          // laptop
          //val apps = Array("application_1511372443652_0002", "application_1511372443652_0003", "application_1511372443652_0001")
          
          if (apps(0)!=""){
                val res = scala.collection.mutable.TreeMap[String,String]()(implicitly[Ordering[String]].reverse)
                var res_html = ""
                
                for (app <- apps) {
                  val url = getJobURL(node_list(0), app)
                  // laptop
                  //val url = getJobURL("wrangler.tacc.utexas.edu", app)
                  println(app)
                  println(url)
                  res(app)=getTbody(url) 
                }
                
                for(key <- res.keys){
                	res_html = res_html + "<div><h3>" + key + "</h3></div>" + "<table>" + res(key) + "</table>"  + "<hr/>"      
                }
                //println(res_html)
                feedback = res_html 
                
            
          }else {
                feedback="No jobs" 
                
          }
      
      
    } catch {
      case e:Exception => {
        feedback = "Failed: wrong reservation name"
      }
    }
    
    feedback
    
 

    }
  
  // description, 
  //tag is div, p, or others to form <div></div>
  
    
}