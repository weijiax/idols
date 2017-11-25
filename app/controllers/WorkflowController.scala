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
    
    var head: String = ""
    
    // eliminate duplicate of tasks when page is refreshed
    if (tasks.size == 0)
      head = buildTasks()

    Ok(views.html.workflow(head, root, tasks.toArray))
  }
  
  
  /**
   * Build tasks
   * Create a new task with name and type
   * Add the task to task ArrayList
   */
  def buildTasks(): String = {
    var workflow = new Workflow()
    
    // read the json object for workflow
    val json = Json.parse(Source.fromFile("public/javascripts/set_up_workflow.js").getLines().mkString)
    
    // update workflow based on the json object 
    workflow.import_JSON(json)
    
    // update tasks
    tasks = workflow.get_tasks()
    return workflow.head
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
  def runTask(index: Integer)  = Action { request =>
    val body = request.body
    val task = tasks(index)
    var feedback: String = ""
    // check tast type
    if (task.taskType.equals("fileUpload")) 
       feedback = tasks(index).run(body)(0); 
    // check if the result of running the task
    if (feedback.substring(0, 7).equals("Success"))
      Ok(feedback)
    else
      BadRequest(feedback)
  }
  
  
//  def runAllTask() = {
//    tasks.foreach(t => t.run())
//  }
  
  
//  def configureTask(t : Task, map : Map[String, Any]) ={
//    t.configure(map)
//  }
  
  
  //def buildTasksFromConfiguraiton(configfilename: String)={}

}