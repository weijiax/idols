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
import forms.SignUpForm
import models.auth.User
import utils.AutoSignUp

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
  passwordHasherRegistry: PasswordHasherRegistry

)(configuration: play.api.Configuration)(
  implicit
  webJarsUtil: WebJarsUtil,
  assets: AssetsFinder
) extends AbstractController(components) with I18nSupport {

  utils.AccountAllocator.init(Json.parse(Source.fromFile(configuration.underlying.getString("training.accounts")).getLines().mkString))

  // clear self generated user file
  val pw = new PrintWriter(configuration.underlying.getString("created.user.path"));
  pw.close();

  // create user accounts that are ready upon start up
  var saver: AutoSignUp = new AutoSignUp(userService, authTokenService, avatarService, credentialsProvider, authInfoRepository, passwordHasherRegistry)
  saver.save_user(Json.parse(Source.fromFile(configuration.underlying.getString("users")).getLines().mkString))

  /**
   * Create an Action to render an HTML page.
   *
   * The configuration in the `routes` file means that this method
   * will be called when the application receives a `GET` request with
   * a path of `/`.
   */
  def signIn() = silhouette.UnsecuredAction.async { implicit request: Request[AnyContent] =>
    Future.successful(Ok(views.html.idols_home()))
  }

  def index() = silhouette.SecuredAction.async { implicit request: SecuredRequest[DefaultEnv, AnyContent] =>
    Future.successful(Ok(views.html.index(request.identity)))
  }

  def show_generate() = silhouette.SecuredAction.async { implicit request: SecuredRequest[DefaultEnv, AnyContent] =>
    Future.successful(Ok(views.html.generate_user(request.identity)))
  }

  /**
   * Generate random users and save users to repository
   */
  var num_user = 1
  def generate_user() = silhouette.SecuredAction.async { implicit request: SecuredRequest[DefaultEnv, AnyContent] =>

    val writer = new BufferedWriter(new FileWriter(configuration.underlying.getString("created.user.path"), true))

    val n = request.body.asMultipartFormData.get.asFormUrlEncoded.get("num").get(0).toInt // number of users to generate

    // json used to sign uo user
    var jsonString: StringBuffer = new StringBuffer
    jsonString.append("{ \"users\": [")

    // json to be displayed to user on screen
    var js: StringBuffer = new StringBuffer
    js.append("{ \"users\": [")

    var password = ""
    for (i <- 1 to n) {
      val (taccName, taccPassword) = utils.AccountAllocator.allocate
      password = scala.util.Random.alphanumeric.take(10).mkString

      jsonString.append("{ \"firstName\":\"train" + num_user + "\",")
      jsonString.append("\"lastName\":\"auto\",")
      jsonString.append("\"username\":\"train" + num_user + "\",")
      jsonString.append("\"password\":\"" + password + "\",") // random String password
      jsonString.append("\"role\":\"UserRole\",")
      jsonString.append("\"taccName\":\"" + taccName + "\",")
      jsonString.append("\"taccPassword\":\"" + taccPassword + "\"")
      jsonString.append("},")

      writer.write("train" + num_user + "\n")
      writer.write(password + "\n")

      js.append("{\"username\":\"train" + num_user + "\",")
      js.append("\"password\":\"" + password + "\",") // random String password
      js.append("\"taccName\":\"" + taccName + "\",")
      js.append("\"taccPassword\":\"" + taccPassword + "\"")
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
    var saver: AutoSignUp = new AutoSignUp(userService, authTokenService, avatarService, credentialsProvider, authInfoRepository, passwordHasherRegistry)
    saver.save_user(Json.parse(jsonString.toString()))

    // display account info
    Future.successful(Ok(Json.prettyPrint(Json.parse(js.toString()))))
  }
}
