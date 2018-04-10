package controllers

import play.api.libs.Files
import javax.inject._
import play.api._
import play.api.mvc._

import java.io._

import scala.io.Source
import models.tasks.Task

import models.Workflow
import models.DirectoryStructure

import play.api.libs.json._

import java.util.ArrayList

import com.mohiva.play.silhouette.api.actions.SecuredRequest
import com.mohiva.play.silhouette.api.{ LogoutEvent, Silhouette }
import org.webjars.play.WebJarsUtil
import play.api.i18n.I18nSupport
import play.api.mvc.{ AbstractController, AnyContent, ControllerComponents }
import utils.auth.DefaultEnv
import scala.concurrent.{ ExecutionContext, Future }

@Singleton
class WorkflowController @Inject() (
  components: ControllerComponents,
  silhouette: Silhouette[DefaultEnv]
)(configuration: play.api.Configuration)(
  implicit
  webJarsUtil: WebJarsUtil,
  assets: AssetsFinder
) extends AbstractController(components) with I18nSupport {

  var tasks = scala.collection.mutable.ArrayBuffer[Task]() // an ArrayList of Tasks
  var directories = new ArrayList[DirectoryStructure]() // an ArrayList of Directory Structures
  var workflow = new Workflow() // one workflow per user
  var workflow_json: String = configuration.underlying.getString("workflow1.json")
  var new_workflow = new Workflow()

  /**
   * An Action to render the Workflow page.
   */
  def showWorkflow() = silhouette.SecuredAction.async { implicit request: SecuredRequest[DefaultEnv, AnyContent] =>

    //    try {
    //      if(workflow.can_generate(Json.parse(Source.fromFile(new_json).getLines().mkString))) {
    //        generate_workflow(new_json, request.identity)
    //      }
    //    } catch {
    //      case e: Exception => {
    //        generate_workflow(workflow_json, request.identity)
    //      }
    //    }

    generate_workflow(workflow_json, request.identity)

    Future.successful(Ok(views.html.workflow.workflow(request.identity, workflow.head, tasks.toArray)))

  }

  /**
   * Generate a workflow based on information retrieved from workflow_json file
   * @param workflow_json: the json file containing workflow information
   */
  def generate_workflow(workflow_json: String, user: models.auth.User) {
    var success = true
    try {
      new_workflow.reset()
      new_workflow.import_JSON(Json.parse(Source.fromFile(workflow_json).getLines().mkString), user)
    } catch {
      case e: Exception => {
        success = false
        e.printStackTrace()
        println("EXCEPTION!!!!")
      }
    }

    if (success) {
      // reset all data in current workflow
      workflow.reset()
      // update workflow based on the json object
      workflow.import_JSON(Json.parse(Source.fromFile(workflow_json).getLines().mkString), user)
    }

    //    // reset all data in current workflow
    //    workflow.reset()
    //
    //    // read the json object for workflow
    //    val json = Json.parse(Source.fromFile(workflow_json).getLines().mkString)
    //
    //    // update workflow based on the json object
    //    workflow.import_JSON(json, user)

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
  def download_workflow() = silhouette.SecuredAction.async {
    println(Json.prettyPrint(workflow.export_JSON()))
    Future.successful(Ok(Json.prettyPrint(workflow.export_JSON())))
  }

  /**
   * Generate directory tree - create a new tree when none of the existing
   * tree matches the one we want
   * @param rootPath:  path to the root of the directory tree
   * @return the String representation of the directory tree as JsValue
   */
  def generateTree(rootPath: String) = silhouette.SecuredAction.async {
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
    Future.successful(Ok(result.getJsValue()))
  }

  /**
   * Run the task
   * @param index: the index of the task in our array of tasks
   * @return the feed back from running the task
   */
  def runTask(index: Integer) = Action { implicit request: Request[AnyContent] =>
    val body = request.body

    if (index == -1) {
      // task: create new workflow with uploaded workflow file
      println(body.asMultipartFormData.get.file("new_workflow"))
      body.asMultipartFormData.get.file("new_workflow").map { new_workflow =>
        workflow_json = new_workflow.ref.getAbsolutePath
        Redirect(routes.WorkflowController.showWorkflow())
      }.getOrElse {
        BadRequest("Something Went Wrong :(")
      }
    } else {

      val task = tasks(index)
      var feedback: String = ""
      feedback = task.run(body);
      // check if the result of running the task
      //      task.task_type match {
      //        case "fileUpload" => { feedback.substring(0, 7) match { case "Success" => Ok(feedback); case _ => BadRequest(feedback) } }
      //        case "checkHadoop" => { feedback.substring(0, 6) match { case "Failed" => BadRequest(feedback); case _ => Ok(feedback) } }
      //        case "runWordCount" => { feedback.substring(0, 6) match { case "Failed" => BadRequest(feedback); case _ => Ok(feedback) } }
      //        case "showResult" => { feedback.substring(0, 6) match { case "Failed" => BadRequest(feedback); case _ => Ok(feedback) } }
      //        case "checkJobStatus" => { feedback.substring(0, 6) match { case "Failed" => BadRequest(feedback); case _ => Ok(feedback) } }
      //        case "startZeppelin" => { feedback.substring(0, 6) match { case "Failed" => BadRequest(feedback); case _ => Ok(feedback) } }
      //        case "runMPI" => { feedback.substring(0, 6) match { case "Failed" => BadRequest(feedback); case _ => Ok(feedback) } }
      //      }
      feedback.substring(0, 6) match { case "Failed" => BadRequest(feedback); case _ => Ok(feedback) }
    }

  }
  
  def getDescription(index: Integer) = silhouette.SecuredAction.async {
    val task = tasks(index)
    Future.successful(Ok(task.get_description()))
  }

  //  def runAllTask() = {
  //    tasks.foreach(t => t.run())
  //  }

  //  def configureTask(t : Task, map : Map[String, Any]) ={
  //    t.configure(map)
  //  }

  //def buildTasksFromConfiguraiton(configfilename: String)={}

}