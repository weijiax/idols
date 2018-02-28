package controllers

import javax.inject._
import play.api._
import play.api.mvc._

import javax.inject.Inject

import play.api.libs.json._
import scala.io.Source

import models.auth.Roles._
import models.auth.WithRole

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
import forms.SignUpForm
import models.auth.User
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

  // create admin user
  save_user(Json.parse(Source.fromFile(configuration.underlying.getString("admin.user")).getLines().mkString))

  /**
   * Create an Action to render an HTML page.
   *
   * The configuration in the `routes` file means that this method
   * will be called when the application receives a `GET` request with
   * a path of `/`.
   */
  //    def index() = silhouette.SecuredAction.async(WithRole(UserRole) || WithRole(AdminRole)) { implicit request: SecuredRequest[DefaultEnv, AnyContent] =>
  def index() = silhouette.SecuredAction.async { implicit request: SecuredRequest[DefaultEnv, AnyContent] =>

    Future.successful(Ok(views.html.index(request.identity)))
  }

  def show_generate() = silhouette.SecuredAction(WithRole(AdminRole)).async { implicit request: SecuredRequest[DefaultEnv, AnyContent] =>
    Future.successful(Ok(views.html.generate_user(request.identity)))
  }

  def save_user(data: JsValue) {
    // loop through all users's info and create users
    var index = 0
    while ((data \ "users" \ index).isInstanceOf[JsDefined]) {
      var authInfo = passwordHasherRegistry.current.hash((data \ "users" \ index \ "password").as[String].replace("\"", ""))
      val loginInfo = LoginInfo(CredentialsProvider.ID, (data \ "users" \ index \ "email").as[String].replace("\"", ""))

      var user = User(
        userID = UUID.randomUUID(),
        loginInfo = loginInfo,
        firstName = Some((data \ "users" \ index \ "firstName").as[String].replace("\"", "")),
        lastName = Some((data \ "users" \ index \ "lastName").as[String].replace("\"", "")),
        fullName = Some((data \ "users" \ index \ "firstName").as[String].replace("\"", "") + " " + (data \ "users" \ index \ "lastName").as[String].replace("\"", "")),
        email = Some((data \ "users" \ index \ "email").as[String].replace("\"", "")),
        //        role = Some((data \ "users" \ index \ "role").as[String].replace("\"", "")),

        role = (data \ "users" \ index \ "role").as[String] match {
          case "UserRole" => UserRole
          case "AdminRole" => AdminRole
        },

        avatarURL = None,
        activated = true
      )

      for {
        avatar <- avatarService.retrieveURL((data \ "users" \ index \ "email").as[String].replace("\"", ""))
        user <- userService.save(user.copy(avatarURL = avatar))
        authInfo <- authInfoRepository.add(loginInfo, authInfo)
        authToken <- authTokenService.create(user.userID)
      } yield {
        //        val url = routes.ActivateAccountController.activate(authToken.id).absoluteURL()
        //        mailerClient.send(Email(
        //          subject = Messages("email.sign.up.subject"),
        //          from = Messages("email.from"),
        //          to = Seq(data.email),
        //          bodyText = Some(views.txt.emails.signUp(user, url).body),
        //          bodyHtml = Some(views.html.emails.signUp(user, url).body)
        //        ))
      }

      index += 1
    }
  }

  /*
   * Generate random users
   */
  def generate_user() = silhouette.SecuredAction.async { implicit request: SecuredRequest[DefaultEnv, AnyContent] =>

    val n = request.body.asMultipartFormData.get.asFormUrlEncoded.get("num").get(0).toInt // number of users to generate
    val r = scala.util.Random

    // save the user information into a json
    var jsonString: StringBuffer = new StringBuffer
    jsonString.append("{ \"users\": [")
    for (i <- 1 to n) {
      jsonString.append("{ \"firstName\":\"training" + i + "\",")
      jsonString.append("\"lastName\":\"auto\",")
      jsonString.append("\"password\":\"" + r.nextInt + "\",") // random int password
      jsonString.append("\"email\":\"training" + i + "@utexas.edu\",")
      jsonString.append("\"role\":\"UserRole\"")
      jsonString.append("},")
    }
    if (n > 0)
      // delete the comma at the end
      jsonString.setLength(jsonString.length() - 1)
    jsonString.append("]}")
    val data = Json.parse(jsonString.toString())

    // create admin from data
    save_user(data)
    Future.successful(Ok(Json.prettyPrint(data)))
  }

}
