package controllers
import javax.inject._
import play.api._
import play.api.mvc._

import models.Task
//import models.UploadTask
//import models.DirectoryStructure
//import models.checkClusterTask
import models._
import play.api.libs.json._

import java.util.ArrayList
import scala.collection.JavaConversions._


@Singleton
class WorkflowController @Inject() (configuration: play.api.Configuration) (cc: ControllerComponents) extends AbstractController(cc) {

  // an ArrayList of Tasks
  var tasks = scala.collection.mutable.ArrayBuffer[Task]()
  // an ArrayList of Directory Structures
  var directories = new ArrayList[DirectoryStructure]()


  /**
   * An Action to render the Workflow page.
   */
  def showWorkflow() = Action { implicit request: Request[AnyContent] =>
    // can change root directory to start the directory tree
    val root = configuration.underlying.getString("fileUpload.default.dir")
    
    // eliminate duplicate of tasks when page is refreshed
    if (tasks.size == 0)
      buildTasks_huang()


    Ok(views.html.workflow(root, tasks.toArray))
  }
  
  
  /**
   * Build tasks
   * Create a new task with name and type
   * Add the task to task ArrayList
   */
  def buildTasks_huang() {
    val task1 = new UploadTask("Preprocessing", "fileUpload")
    //val task2 = new UploadTask("Data Analysis", "fileUpload")
    //val task3 = new UploadTask("Postprocessing", "fileUpload")
    val task4 = new checkClusterTask("Cluster Status","checkHadoop")
    val task5 = new runWordCountTask("Word Count Example","runWordCount")
    val task6 = new checkHadoopJobStatusTask("Job Status","checkJobStatus")
    val task7 = new showResultTask("Show result","showResult")
    val task8 = new startZeppelinTask("Lauch Zeppelin","startZeppelin")

    tasks.append(task1)
    //tasks.append(task2)
    //tasks.append(task3)
    tasks.append(task4)
    tasks.append(task5)
    tasks.append(task6)
    tasks.append(task7)
    tasks.append(task8)

  }

  /**
   * Generate directory tree - create a new tree when none of the existing 
   * tree matches the one we want
   * @param rootPath:  path to the root of the directory tree
   * @return the String representation of the directory tree as JsValue
   */
  def generateTree(rootPath: String) = Action {
    var result: DirectoryStructure = null
    if (directories.size() == 0) {
      // No dirTree saved yet, create the tree and save it
      result = new DirectoryStructure(rootPath)
      directories.add(result)
    } else {
      // loop through existing directory trees to search for one with the same root
      for (index <- 0 until directories.size()) {
        var dirTree = directories.get(index)
        if (rootPath.equals(dirTree.root.name))
          result = dirTree
      }
      if (result == null) {
        // None of directory trees starts with this root path, create a new one
        result = new DirectoryStructure(rootPath)
        directories.add(result)
      }
    }
    Ok(result.getJsValue())
  }
  
  /**
   * Run the task
   * @param index: the index of the task in our array of tasks
   * @return the feed back from running the task
   */
  def runTask(index: Integer)  = Action { implicit request: Request[AnyContent] =>
    val body = request.body
    val task = tasks(index)
    var feedback: String = ""
    // check tast type
//    if (task.taskType.equals("fileUpload")){ 
    feedback = task.run(body); 
        // check if the result of running the task
    task.taskType match {
      case "fileUpload"       => {feedback.substring(0, 7) match {case "Success" => Ok(feedback); case _ => BadRequest(feedback)} }
      case "checkHadoop"      => { Ok(feedback) }
      //case "runWordCount" => {feedback match {case "Job finished" => Ok(feedback); case _ => BadRequest(feedback)} }
      case "runWordCount"     => {Ok("Job submitted with process ID: "+feedback)}
      case "showResult"       => {Ok(feedback)}
      case "checkJobStatus"   => {Ok(feedback)}
      case "startZeppelin"    => {Ok(feedback)}
    }

//    }
//    else if (task.taskType.equals("checkHadoop")){
//       feedback = tasks.get(index).run(body); 
//       Ok(feedback)      
//    } else {
//      Ok("ok")
//    }
    
    
   

  }
  
  
//  def runAllTask() = {
//    tasks.foreach(t => t.run())
//  }
  
  
//  def configureTask(t : Task, map : Map[String, Any]) ={
//    t.configure(map)
//  }
  
  
  //def buildTasksFromConfiguraiton(configfilename: String)={}

}