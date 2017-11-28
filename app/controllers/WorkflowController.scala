package controllers
import javax.inject._
import play.api._
import play.api.mvc._

import scala.io.Source
import models.Task

import models.Workflow
import models.DirectoryStructure

import play.api.libs.json._

import java.util.ArrayList


@Singleton
class WorkflowController @Inject() (configuration: play.api.Configuration) (cc: ControllerComponents) extends AbstractController(cc) {

  var tasks = scala.collection.mutable.ArrayBuffer[Task]()  // an ArrayList of Tasks
  var directories = new ArrayList[DirectoryStructure]()  // an ArrayList of Directory Structures
  var workflow = new Workflow()  // one workflow per user


  /**
   * An Action to render the Workflow page.
   */
  def showWorkflow() = Action { implicit request: Request[AnyContent] =>
    // can change root directory to start the directory tree
    val root = configuration.underlying.getString("fileUpload.default.dir")
    val workflow_json = configuration.underlying.getString("workflow1.json")


    // generate workflow with json 
    generate_workflow(workflow_json)

    Ok(views.html.workflow(workflow.head, root, tasks.toArray))
  }
  
  /**
   * Generate a workflow based on information retrieved from workflow_json file
   * @param workflow_json: the json file containing workflow information
   */
  def generate_workflow(workflow_json: String) {
    // reset all data in current workflow
    workflow.reset()
    
    // read the json object for workflow
    val json = Json.parse(Source.fromFile(workflow_json).getLines().mkString)
    
    // update workflow based on the json object 
    workflow.import_JSON(json)
    
    // build task based on current workflow
    buildTasks()
  }
  
  /**
   * Build tasks
   * Create a new task with name and type
   * Add the task to task ArrayList
   */

  def buildTasks() {
    // update tasks
    tasks = workflow.get_tasks()

  }
  


  
  /**
   * Download current workflow as a json file
   */
  def download_workflow() = Action {
    Ok(workflow.export_JSON())
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

    
   

  }
  
  

//  def runAllTask() = {
//    tasks.foreach(t => t.run())
//  }
  
  
//  def configureTask(t : Task, map : Map[String, Any]) ={
//    t.configure(map)
//  }

  //def buildTasksFromConfiguraiton(configfilename: String)={}

}