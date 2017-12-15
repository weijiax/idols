package models

import play.api.mvc._
import models.helperFunctions._
import scala.sys.process._


class runMPITask(name: String, tType: String) extends Task(name, tType){
   //name of this task, example: preprocessing, data analysis, postprocessing
  val taskName = name
  // type of this task, example: fileUpload
  val taskType = tType
  
  
  
  
//  var file : File 
//  var target : String 
//  
  def run(body: AnyContent) : String= {
    runMPI(body)
  }
  
    /**
   * check Cluster info, node list, etc.
   */
  def runMPI(body: AnyContent) : String = {
    var feedback = ""
    
    val opt = body.asFormUrlEncoded
    // job.mpi dir, also output dir 
    val dir = opt.get("dir")(0) 
    
    // variables to replace in job.mpi using sed
    val mpi_script = opt.get("mpi_script")(0) 
    
    val split_script = opt.get("split_script")(0) 
    
    val combine_script = opt.get("combine_script")(0) 
    
    val no_mpi = !new java.io.File(dir + "/" + mpi_script).exists
    val no_split = !new java.io.File(dir + "/" + split_script).exists
    val no_combine = !new java.io.File(dir + "/" + combine_script).exists
    
    if (new java.io.File(dir).exists) {
      if (no_mpi && no_split && no_combine){
        feedback = "Failed: MPI, split and combine script path not exist. "
      }
      if (no_mpi && no_split && !no_combine){
        feedback = "Failed: MPI, split script path not exists. "
      }
      if (no_mpi && !no_split && no_combine){
        feedback = "Failed: MPI, combine script path not exists. "
      }
      if (!no_mpi && no_split && no_combine){
        feedback = "Failed: split and combine script path not exists. "
      }
      if (no_mpi && !no_split && !no_combine){
        feedback = "Failed: MPI script path not exists. "
      } 
      if (!no_mpi && no_split && !no_combine){
        feedback = "Failed: split script path not exists. "
      }
      if (!no_mpi && !no_split && no_combine){
        feedback = "Failed: combine script path not exists. "
      }
    }else {
      feedback= "Failed: directory does not exists. "      
    }

    
    
    // sbatch variables
    val job_name = opt.get("job_name")(0)     
    val num_nodes = opt.get("num_nodes")(0) 
    val total_tasks = opt.get("total_tasks")(0)
    val queue_name = opt.get("queue_name")(0)
    val stdout_name = opt.get("stdout_name")(0)
    val run_time = opt.get("run_time")(0)
    val allocation =  opt.get("allocation")(0)
    

    
    
    val command = (
        "cd " + dir + "; sed 's/MPI_script/" + mpi_script + 
        "/g; s/split/" + split_script + "/g; s/combine/" + 
        combine_script + "/g' job.mpi > job.mpi.submit"
        )

    val out = Process(Seq("bash","-c", command)).!  // out ==0; success

    val command_1 = (
        "ssh -n login1 'cd " + dir + ";ml impi; sbatch -J " + job_name +" -N " + 
        num_nodes + " -n " + total_tasks + " -p " + queue_name + " -o " + stdout_name + 
        " -t " + run_time + " -A " + allocation + " job.mpi.submit'"
        )
          
    if (out==0 && feedback ==""){
      val res = Process(Seq("bash","-c", command_1)).!!.split("\n")
      feedback = arrayToHtml("Sbatch info:",res,"div")      
    }

    
    feedback

  }
  
  // description, 
  //tag is div, p, or others to form <div></div>
  
    
}