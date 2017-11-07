package controllers
import javax.inject._
import play.api._
import play.api.mvc._

import models.Task
import models.UploadTask
import models.DirectoryStructure
import play.api.libs.json._

import java.util.ArrayList


@Singleton
class WorkflowController @Inject() (configuration: play.api.Configuration) (cc: ControllerComponents) extends AbstractController(cc) {

  var tasks = new ArrayList[Task]()
//  var directories : Map[String, JsValue]
  
  
  def showWorkflow() = Action { implicit request: Request[AnyContent] =>
    // can change root directory to start the directory tree
    val root = configuration.underlying.getString("fileUpload.default.dir")
    
    // eliminate duplicate of tasks when page is refreshed
    if (tasks.size() == 0)
      buildTasks()

    var taskArray: Array[Task] = new Array[Task](tasks.size())
    taskArray = tasks.toArray(taskArray)
    // request root path & conf
    Ok(views.html.workflow(root, taskArray))
  }
  
  
  def buildTasks() {
    val task1 = new UploadTask("Preprocessing", "fileUpload")
    val task2 = new UploadTask("Data Analysis", "fileUpload")
    val task3 = new UploadTask("Postprocessing", "fileUpload")
    tasks.add(task1)
    tasks.add(task2)
    tasks.add(task3)
  }
  
  
  var dirTree: DirectoryStructure = null
  def generateTree(rootPath: String) = Action {
    if (dirTree == null || !rootPath.equals(dirTree.root.name))
      dirTree = new DirectoryStructure(rootPath)
    Ok(dirTree.getJsonString())
  }
  
  
  def runTask(index: Integer)  = Action(parse.multipartFormData) { request =>
    val body = request.body
    val task = tasks.get(index)
    if (task.taskType.equals("fileUpload"))
      tasks.get(index).run(body)

    Ok("File Uploaded");
  }
  
  
//  def runAllTask() = {
//    tasks.foreach(t => t.run())
//  }
  
  
//  def configureTask(t : Task, map : Map[String, Any]) ={
//    t.configure(map)
//  }
  
  
  //def buildTasksFromConfiguraiton(configfilename: String)={}

}