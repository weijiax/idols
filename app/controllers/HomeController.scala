package controllers

import javax.inject._
import play.api._
import play.api.mvc._

import javax.inject.Inject

import play.api.libs.json._
import scala.io.Source
import java.io.File
import java.io.FileWriter
import java.io.BufferedWriter
import java.io.PrintWriter

import models.auth.Roles._
import models.auth.WithRole
import models.auth.User
import models.auth.TaccCredential
import utils.AutoSignUp
import utils.NotebookAllocator

import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.api.actions.SecuredRequest
import com.mohiva.play.silhouette.api.{ LogoutEvent, Silhouette }
import com.mohiva.play.silhouette.api.util.{ Clock, Credentials }
import com.mohiva.play.silhouette.impl.providers._

import org.webjars.play.WebJarsUtil
import play.api.i18n.I18nSupport
import play.api.mvc.{ AbstractController, AnyContent, ControllerComponents }
import utils.auth.DefaultEnv
import scala.concurrent.{ ExecutionContext, Future }
import scala.sys.process._
import scala.collection.Seq

import java.util.UUID
import javax.inject.Inject

import com.mohiva.play.silhouette.api._
import com.mohiva.play.silhouette.api.services.AvatarService
import com.mohiva.play.silhouette.api.util.PasswordHasherRegistry

import models.services.{ AuthTokenService, UserService }
import org.webjars.play.WebJarsUtil

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject() (
  components: ControllerComponents,
  silhouette: Silhouette[DefaultEnv],
  userService: UserService,
  authTokenService: AuthTokenService,
  avatarService: AvatarService,
  credentialsProvider: CredentialsProvider,
  authInfoRepository: AuthInfoRepository,
  passwordHasherRegistry: PasswordHasherRegistry)(configuration: play.api.Configuration)(
  implicit
  webJarsUtil: WebJarsUtil,
  assets: AssetsFinder) extends AbstractController(components) with I18nSupport {

  utils.AccountAllocator.initTacc(Json.parse(Source.fromFile(configuration.underlying.getString("training.accounts")).getLines().mkString))

  // clear self generated user file
  val pw1 = new PrintWriter(configuration.underlying.getString("created.user.path"));
  pw1.close();

  val pw2 = new PrintWriter(configuration.underlying.getString("jupyter.sessions"));
  pw2.close();

  // create user accounts that are ready upon start up
  utils.AutoSignUp.save_user(userService, authTokenService, avatarService, credentialsProvider, authInfoRepository, passwordHasherRegistry, Json.parse(Source.fromFile(configuration.underlying.getString("users")).getLines().mkString))

  /**
   * home page - an introduction to idols
   */
  def home() = silhouette.UserAwareAction.async { implicit request: Request[AnyContent] =>
    Future.successful(Ok(views.html.idols_home()))
  }

  /**
   * use cases page
   */
  def use_cases() = silhouette.SecuredAction.async { implicit request: SecuredRequest[DefaultEnv, AnyContent] =>
    val cases = Source.fromFile(configuration.underlying.getString("use.cases")).getLines().mkString
    Future.successful(Ok(views.html.use_cases(request.identity, cases)))
  }

  /**
   * contact page
   */
  def contact() = silhouette.UserAwareAction.async { implicit request: Request[AnyContent] =>
    Future.successful(Ok(views.html.contact()))
  }

  def send_feedback() = silhouette.UserAwareAction.async { implicit request: Request[AnyContent] =>
    val body = request.body.asFormUrlEncoded
    print(body)
    val name: String = body.get("name")(0)
    val email: String = body.get("email")(0)
    val feedback: String = body.get("feedback")(0)
    val cmd = "echo '" + feedback + "' | mailx -s 'Feedback - " + name + "' -r '" + name + "<" + email + ">' " + configuration.underlying.getString("contact.email")
    print(cmd)
    val res = Process(Seq("bash", "-c", cmd)).!
    print(res)
    Future.successful(Ok(""))
  }

  /**
   * generate user page
   */
  def show_generate_user() = silhouette.SecuredAction.async { implicit request: SecuredRequest[DefaultEnv, AnyContent] =>
    Future.successful(Ok(views.html.generate_user(request.identity)))
  }

  /**
   * Generate random users and save users to repository
   */
  var num_user = 31
  def generate_user() = silhouette.SecuredAction.async { implicit request: SecuredRequest[DefaultEnv, AnyContent] =>

    val writer = new BufferedWriter(new FileWriter(configuration.underlying.getString("created.user.path"), true))

    val n = request.body.asMultipartFormData.get.asFormUrlEncoded.get("num").get(0).toInt // number of users to generate

    // json used to sign up user
    var jsonString: StringBuffer = new StringBuffer
    jsonString.append("{ \"users\": [")

    // json to be displayed to user on screen
    var js: StringBuffer = new StringBuffer
    js.append("{ \"users\": [")

    var password = ""
    for (i <- 1 to n) {
      password = scala.util.Random.alphanumeric.take(10).mkString

      jsonString.append("{ \"firstName\":\"train" + num_user + "\",")
      jsonString.append("\"lastName\":\"auto\",")
      jsonString.append("\"username\":\"train" + num_user + "\",")
      jsonString.append("\"password\":\"" + password + "\",") // random String password
      jsonString.append("\"role\":\"UserRole\"")
      jsonString.append("},")

      writer.write("train" + num_user + "\n")
      writer.write(password + "\n")

      js.append("{\"username\":\"train" + num_user + "\",")
      js.append("\"password\":\"" + password + "\"") // random String password
      js.append("},")

      num_user += 1
    }
    writer.close()

    if (n > 0) {
      // delete the comma at the end
      jsonString.setLength(jsonString.length() - 1)
      js.setLength(js.length() - 1)
    }

    jsonString.append("]}")
    js.append("]}")

    // create Users from json
    utils.AutoSignUp.save_user(userService, authTokenService, avatarService, credentialsProvider, authInfoRepository, passwordHasherRegistry, Json.parse(jsonString.toString()))

    // display account info
    Future.successful(Ok(Json.prettyPrint(Json.parse(js.toString()))))
  }

  def show_submit_script() = silhouette.SecuredAction.async { implicit request: SecuredRequest[DefaultEnv, AnyContent] =>
    Future.successful(Ok(views.html.submit_script(request.identity)))
  }

  var submitted = false
  def submit_script() = silhouette.SecuredAction.async { implicit request: SecuredRequest[DefaultEnv, AnyContent] =>
    if (submitted) {
      Future.successful(Ok("Already submitted, please wait for it to finish"))
    }
    submitted = true

    val json = Json.parse(request.body.asText.get)
    val action = (json \ "action").as[String]
    val path = (json \ "path").as[String]
    println(action)
    println(path)

    val allTACC = utils.AccountAllocator.getAll()
    for (acc <- allTACC) {
      val username = acc.username
      val password = acc.password

      val command = "ssh -n login1 \" echo '" + password.replace("$", "\\$") + "' | su - " + username + " -c '" + path + "'\""
      Process(Seq("bash", "-c", command)).!!
    }

    submitted = false
    Future.successful(Ok("Finished submission"))
  }

  /**
   * Generate random users and save users to repository
   */
  var port_num = 56000

  def show_job_management() = silhouette.SecuredAction.async { implicit request: SecuredRequest[DefaultEnv, AnyContent] =>
    Future.successful(Ok(views.html.job_management(request.identity)))
  }

  def job_management() = silhouette.SecuredAction.async { implicit request: SecuredRequest[DefaultEnv, AnyContent] =>
    val json = Json.parse(request.body.asText.get)
    val action = (json \ "action").as[String]
    if (action == "refresh") {
      Future.successful(Ok(refresh_jobs()))
    } else if (action == "launch") {
      println("launch")

      var str = (json \ "selected").as[String]
      Future.successful(Ok(launch_jobs(str)))
    } else {
      var str = (json \ "selected").as[String]
      Future.successful(Ok(cancel_jobs(str)))
    }
  }

  def refresh_jobs(): String = {
    val allTACC = utils.AccountAllocator.getAll()
    var js: StringBuffer = new StringBuffer
    js.append("[")
    for (acc <- allTACC) {
      js.append(acc.getJobStatus())
    }
    js.setLength(js.length() - 1)
    js.append("]")
    js.toString()
  }

  var launched = false
  def launch_jobs(str: String): String = {
    if (launched) {
      "Launched once, please wait for it to finish"
    }
    launched = true
    // json to be displayed to user on screen
    var js: StringBuffer = new StringBuffer
    js.append("{ \"Jupyter Notebook Sessions\": [")
    var selected = Json.parse(str)
    var index = 0
    while ((selected \ index).isInstanceOf[JsDefined]) {
      val username = (selected \ index \ 3).as[String].replace("\"", "")
      println(username)

      val acc = utils.AccountAllocator.credentialMapping.get(username).get

      //      if (!acc.jobs.contains("jupyter")) {
      acc.jobs += "jupyter"
      val password = acc.password

      val jupyter_password = scala.util.Random.alphanumeric.take(10).mkString

      val command = "ssh -n login1 \" echo '" + password.replace("$", "\\$") + "' | su - " + username + " -c 'cp /work/00791/xwj/DDL_SC18/{jupyter.job,setup_jupyter.py,launch_jupyter.sh} ~/ ; ./launch_jupyter.sh " + jupyter_password + " " + port_num + "'\""
      Process(Seq("bash", "-c", command)).!

      js.append("{\"account\":\"" + username + "\",")
      js.append("\"port\":\"" + port_num + "\",")
      js.append("\"password\":\"" + jupyter_password + "\"")
      js.append("},")

      port_num += 1
      //      }
      index += 1
    }

    // delete the comma at the end
    js.setLength(js.length() - 1)

    js.append("]}")
    utils.NotebookAllocator.initJupyterNotebook(Json.parse(js.toString()))
    launched = false
    "Finished launching"
  }

  def cancel_jobs(str: String): String = {

    // json to be displayed to user on screen
    var js: StringBuffer = new StringBuffer
    js.append("{ \"Jupyter Notebook Sessions\": [")
    var selected = Json.parse(str)
    var index = 0
    while ((selected \ index).isInstanceOf[JsDefined]) {
      val pid = (selected \ index \ 0).as[String].replace("\"", "")
      val username = (selected \ index \ 3).as[String].replace("\"", "")
      val acc = utils.AccountAllocator.credentialMapping.get(username).get

      val command1 = "squeue -u " + username + " -j " + pid
      var response = Process(Seq("bash", "-c", command1)).!!.split("\n")
      if (response.length > 1) {
        val password = acc.password

        val port = (selected \ index \ 8).as[String].replace("\"", "")

        val command = "ssh -n login1 \" echo '" + password.replace("$", "\\$") + "' | su - " + username + " -c 'scancel " + pid + "'\""
        Process(Seq("bash", "-c", command)).!!.split("\n")

        if (port != "") {
          utils.NotebookAllocator.removeNotebook(username, port)
        }
      }

      index += 1
    }

    "Finished canceling"
  }

}

