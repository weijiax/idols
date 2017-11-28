package models

import play.api.mvc._
import sys.process._


class runWordCountTask(name: String, tType: String) extends Task(name, tType){
   //name of this task, example: preprocessing, data analysis, postprocessing
  val taskName = name
  // type of this task, example: fileUpload
  val taskType = tType
  
//  var file : File 
//  var target : String 
//  
  def run(body: AnyContent) : String= {
    runWordCount(body)
  }
  
    /**
   * check Cluster info, node list, etc.
   */
  def runWordCount(body: AnyContent) : String = {
    val userInput = body.asFormUrlEncoded
    val script_path = userInput.get("script_path")(0)
    val input_file_path = userInput.get("input_file_path")(0)
    val output_path = userInput.get("output_path")(0)

    val command = "source " + script_path + " " + input_file_path + " " + output_path + " &> /dev/null  & echo $!"
    val res = Process(Seq("bash","-c", command)).!!.split("\n")(0)
    
    res
    
//    res match {
//      case 0 => "Job finished"
//      case _ => "Something went wrong"
//    }
  }
    
}