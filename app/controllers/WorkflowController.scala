package controllers
import javax.inject._
import play.api._
import play.api.mvc._

import model.Task
import play.api.libs.json._

class WorkflowController @Inject()(cc: ControllerComponents) extends AbstractController(cc) {
  var tasks : Array[Task]
  var directories : Map[String, JsValue]
  
  def runTask(task : Task) =  { 
    task.run()
  }
  
  def runAllTask() = {
    tasks.foreach(t => t.run())
  }
  
  def configureTask(t : Task, map : Map[String, Any]) ={
    t.configure(map)
  }
  
  
  def buildTaks() = { }
  
  //def buildTasksFromConfiguraiton(configfilename: String)={}
  
  def showWorkflow  = Action{
    this.buildTaks()
    Ok(views.html.workflow(tasks))
  }
  
  
  
  
}