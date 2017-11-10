package controllers
import javax.inject._
import play.api._
import play.api.mvc._

import models.Task
import models.UploadTask
import models.DirectoryStructure
import play.api.libs.json._

import java.util.ArrayList
import scala.collection.JavaConversions._


@Singleton
class WorkflowController @Inject() (configuration: play.api.Configuration) (cc: ControllerComponents) extends AbstractController(cc) {

  var tasks = new ArrayList[Task]()
  var directories = new ArrayList[DirectoryStructure]()

  /**
   * An Action to render the Workflow page.
   */
  def showWorkflow() = Action { implicit request: Request[AnyContent] =>
    // can change root directory to start the directory tree
    val root = configuration.underlying.getString("fileUpload.default.dir")
    
    // eliminate duplicate of tasks when page is refreshed
    if (tasks.size() == 0)
      buildTasks()

    var taskArray: Array[Task] = new Array[Task](tasks.size())
    taskArray = tasks.toArray(taskArray)

    Ok(views.html.workflow(root, taskArray))
  }
  
  
  /**
   * Build tasks
   */
  def buildTasks() {
    val task1 = new UploadTask("Preprocessing", "fileUpload")
    val task2 = new UploadTask("Data Analysis", "fileUpload")
    val task3 = new UploadTask("Postprocessing", "fileUpload")
    tasks.add(task1)
    tasks.add(task2)
    tasks.add(task3)
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
    val task = tasks.get(index)
    var feedback: String = ""
    if (task.taskType.equals("fileUpload"))
       feedback = tasks.get(index).run(body)   
    Ok(feedback)
  }
  
  
//  def runAllTask() = {
//    tasks.foreach(t => t.run())
//  }
  
  
//  def configureTask(t : Task, map : Map[String, Any]) ={
//    t.configure(map)
//  }
  
  
  //def buildTasksFromConfiguraiton(configfilename: String)={}

}