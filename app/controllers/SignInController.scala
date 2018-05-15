package controllers

import utils.AutoSignUp
import javax.inject.Inject

import com.mohiva.play.silhouette.api.Authenticator.Implicits._
import com.mohiva.play.silhouette.api._
import com.mohiva.play.silhouette.api.exceptions.ProviderException
import com.mohiva.play.silhouette.api.util.{ Clock, Credentials }
import com.mohiva.play.silhouette.impl.exceptions.IdentityNotFoundException
import com.mohiva.play.silhouette.impl.providers._
import com.mohiva.play.silhouette.api.services.AvatarService
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.api.util.PasswordHasherRegistry

import forms.SignInForm
import models.services.{ AuthTokenService, UserService }

import net.ceedubs.ficus.Ficus._
import org.webjars.play.WebJarsUtil
import play.api.Configuration
import play.api.i18n.{ I18nSupport, Messages }
import play.api.mvc.{ AbstractController, AnyContent, ControllerComponents, Request }
import utils.auth.DefaultEnv

import scala.concurrent.duration._
import scala.concurrent.{ ExecutionContext, Future }
import scala.sys.process._
import play.api.libs.json._

/**
 * The `Sign In` controller.
 *
 * @param components             The Play controller components.
 * @param silhouette             The Silhouette stack.
 * @param userService            The user service implementation.
 * @param credentialsProvider    The credentials provider.
 * @param socialProviderRegistry The social provider registry.
 * @param configuration          The Play configuration.
 * @param clock                  The clock instance.
 * @param webJarsUtil            The webjar util.
 * @param assets                 The Play assets finder.
 */
class SignInController @Inject() (
  components: ControllerComponents,
  silhouette: Silhouette[DefaultEnv],
  userService: UserService,
  authTokenService: AuthTokenService,
  avatarService: AvatarService,
  credentialsProvider: CredentialsProvider,
  authInfoRepository: AuthInfoRepository,
  passwordHasherRegistry: PasswordHasherRegistry,
  socialProviderRegistry: SocialProviderRegistry,
  configuration: Configuration,
  clock: Clock)(
  implicit
  webJarsUtil: WebJarsUtil,
  assets: AssetsFinder,
  ex: ExecutionContext) extends AbstractController(components) with I18nSupport {

  /**
   * Views the `Sign In` page.
   *
   * @return The result to display.
   */
  def view = silhouette.UnsecuredAction.async { implicit request: Request[AnyContent] =>
    Future.successful(Ok(views.html.signIn(SignInForm.form, socialProviderRegistry)))
  }

  /**
   * Handles the submitted form.
   *
   * @return The result to display.
   */
  def submit = silhouette.UnsecuredAction.async { implicit request: Request[AnyContent] =>
    SignInForm.form.bindFromRequest.fold(
      form => Future.successful(BadRequest(views.html.signIn(form, socialProviderRegistry))),
      data => {
        if (request.body.asFormUrlEncoded.get("action")(0).equals("tacc")) {
          // authorize user with input
          val username: String = data.email
          val password: String = data.password
          var cmd = Seq("curl", "-X", "POST", "-u", "_zVxwGJfexDkmSnUT1e7y2mLYAIa:IUokd8ceXpoPuwvNpgnOm4bB0_ga", "-d",
            "grant_type=password", "-d", s"username=$username", "-d", s"password=$password", "-d", "scope=PRODUCTION",
            "https://api.tacc.utexas.edu/token")
          // execute curl command and retrieve response
          var response = cmd.!!

          if (!response.startsWith("{\"error\"")) {
            // user authorized, use access_token get user profile by executing another curl command
            val access_token = (Json.parse(response) \ "access_token").as[String].replace("\"", "")
            println(access_token)
            cmd = Seq("curl", "-H", s"Authorization: Bearer $access_token",
              "https://api.tacc.utexas.edu/profiles/v2/me")
            response = cmd.!!

            // Create a json string with info of this user
            val user_info: JsValue = Json.obj(
              "users" -> Json.arr(
                Json.obj(
                  "firstName" -> (Json.parse(response) \ "result" \ "first_name").as[String].replace("\"", ""),
                  "lastName" -> (Json.parse(response) \ "result" \ "last_name").as[String].replace("\"", ""),
                  "password" -> password,
                  "email" -> username,
                  "access_token" -> access_token,
                  "role" -> "UserRole")))

            // Call command to save (sign up) user
            var saver: AutoSignUp = new AutoSignUp(userService, authTokenService, avatarService, credentialsProvider, authInfoRepository, passwordHasherRegistry)
            saver.save_user(user_info)

          }
        }

        Thread.sleep(100)

        val credentials = Credentials(data.email, data.password)
        credentialsProvider.authenticate(credentials).flatMap { loginInfo =>
          val result = Redirect(routes.HomeController.index())
          userService.retrieve(loginInfo).flatMap {
            case Some(user) if !user.activated =>
              Future.successful(Ok(views.html.activateAccount(data.email)))
            case Some(user) =>
              val c = configuration.underlying
              silhouette.env.authenticatorService.create(loginInfo).map {
                case authenticator => authenticator
              }.flatMap { authenticator =>
                silhouette.env.eventBus.publish(LoginEvent(user, request))
                silhouette.env.authenticatorService.init(authenticator).flatMap { v =>
                  silhouette.env.authenticatorService.embed(v, result)
                }

              }
            case None => Future.failed(new IdentityNotFoundException("Couldn't find user"))
          }
        }.recover {
          case _: ProviderException =>
            Redirect(routes.SignInController.view()).flashing("error" -> Messages("invalid.credentials"))
        }
      })
  }

  /**
   *  Handle user signin from facebook 
   */
  def facebookLogin(response: String, accessToken: String) = silhouette.UnsecuredAction.async { implicit request: Request[AnyContent] =>
    val email = (Json.parse(response) \ "email").as[String].replace("\"", "")
    val password = scala.util.Random.alphanumeric.take(10).mkString
    val user_info: JsValue = Json.obj(
      "users" -> Json.arr(
        Json.obj(
          "firstName" -> (Json.parse(response) \ "name").as[String].replace("\"", "").split(" ")(0),
          "lastName" -> (Json.parse(response) \ "name").as[String].replace("\"", "").split(" ")(1),
          "password" -> password,
          "email" -> email,
          "access_token" -> accessToken,
          "role" -> "UserRole")))

    // Call command to save (sign up) user
    var saver: AutoSignUp = new AutoSignUp(userService, authTokenService, avatarService, credentialsProvider, authInfoRepository, passwordHasherRegistry)
    saver.save_user(user_info)
    
    Thread.sleep(100)
    val credentials = Credentials(email, password)
    credentialsProvider.authenticate(credentials).flatMap { loginInfo =>
      val result = Redirect(routes.HomeController.index())
      userService.retrieve(loginInfo).flatMap {
        case Some(user) if !user.activated =>
          Future.successful(Ok(views.html.activateAccount(email)))
        case Some(user) =>
          val c = configuration.underlying
          silhouette.env.authenticatorService.create(loginInfo).map {
            case authenticator => authenticator
          }.flatMap { authenticator =>
            silhouette.env.eventBus.publish(LoginEvent(user, request))
            silhouette.env.authenticatorService.init(authenticator).flatMap { v =>
              silhouette.env.authenticatorService.embed(v, result)
            }
          }
        case None => Future.failed(new IdentityNotFoundException("Couldn't find user"))
      }
    }.recover {
      case _: ProviderException =>
        Redirect(routes.SignInController.view()).flashing("error" -> Messages("invalid.credentials"))
    }

  }

}
