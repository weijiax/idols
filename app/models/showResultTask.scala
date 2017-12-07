package models

import play.api.mvc._
import sys.process._
import models.helperFunctions._
 


class showResultTask(name: String, tType: String) extends Task(name, tType){
   //name of this task, example: preprocessing, data analysis, postprocessing
  val taskName = name
  // type of this task, example: fileUpload
  val taskType = tType
  
  private var feedback = ""
  
//  var file : File 
//  var target : String 
//  
  def run(body: AnyContent) : String= {
    showOutput(body)
  }
  
    /**
   * check Cluster info, node list, etc.
   */
  def showOutput(body: AnyContent) : String = {
    val userInput = body.asFormUrlEncoded
    val output_path = userInput.get("output_path")(0)
    val top_n = userInput.get("top_n")(0)
    
    // test if HDFS path exists
    val command_0 = "hdfs dfs -test -d " + output_path
    // test = 0 exist, test=1 not exist
    val test = Process(Seq("bash","-c", command_0)).!
    
    if (test==0){ // if path exist
          //laptop
      //val command = "head -n " + top_n + " " + output_path
      val command = "hadoop fs -cat " + output_path + "/part-r-00000 | head -n " + top_n
      
      //need error handler here...
      val res = Process(Seq("bash","-c", command)).!!.split("\n")
      
      feedback = arrayToHtml("Results:",res,"div")
      
    }else { // if path not exist
      feedback = "Failed: HDFS path does not exist. "
    }
    feedback

  }
    
}