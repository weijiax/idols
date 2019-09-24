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
import com.mohiva.play.silhouette.api.LoginInfo

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
import scala.io.Source
import scala.collection.mutable.HashMap
import scala.collection.Iterator
import scala.util.control.Breaks._

import java.io.File
import java.io.FileWriter
import java.io.BufferedWriter

import play.api.libs.json._

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory


import collection.JavaConverters._
import scala.util.Try


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

  // save admin accounts
  var index = 0
  var admins = scala.collection.mutable.ArrayBuffer[String]() // an ArrayList of Tasks
  val json2 = Json.parse(Source.fromFile(configuration.underlying.getString("admins")).getLines().mkString)
  index = 0
  while ((json2 \ "admin_accounts" \ index).isInstanceOf[JsDefined]) {
    admins += (json2 \ "admin_accounts" \ index \ "username").as[String].replace("\"", "")
    index += 1
  }
  
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
          // Authenticate TACC account through Agave
          val username: String = data.username
          val password: String = data.password

          // encode special characters (% and &)
          val tempPassword: String = password.replaceAll("%", "%25").replaceAll("&", "%26")

          var cmd = Seq("curl", "-X", "POST", "-u", "_zVxwGJfexDkmSnUT1e7y2mLYAIa:IUokd8ceXpoPuwvNpgnOm4bB0_ga", "-d",
            "grant_type=password", "-d", s"username=$username", "-d", s"password=$tempPassword", "-d", "scope=PRODUCTION",
            "https://api.tacc.utexas.edu/token")
            
//           var cmd = Seq("curl", "-X", "POST", "-u", "3Xw1YcSK1ksxsZbMaHL3MJNeYTUa:ZbGWbBBDF4GeztJWdbmgmaRLWNIa", "-d",
//            "grant_type=password", "-d", s"username=$username", "-d", s"password=$tempPassword", "-d", "scope=PRODUCTION",
//            "https://api.tacc.utexas.edu/token")
            
            
          // execute curl command and retrieve response
          var response = cmd.!!
          

          if (!response.startsWith("{\"error\"")) {
            // user authorized, use access_token to get user profile by executing another curl command
            val access_token = (Json.parse(response) \ "access_token").as[String].replace("\"", "")
            cmd = Seq("curl", "-H", s"Authorization: Bearer $access_token",
              "https://api.tacc.utexas.edu/profiles/v2/me")
            response = cmd.!!

            // determine whether this TACC account should be an admin
            val role = if (admins.contains(username.toLowerCase)) "AdminRole" else "UserRole"

            // Create a json string with info of this user
            val user_info: JsValue = Json.obj(
              "users" -> Json.arr(
                Json.obj(
                  "firstName" -> (Json.parse(response) \ "result" \ "first_name").as[String].replace("\"", ""),
                  "lastName" -> (Json.parse(response) \ "result" \ "last_name").as[String].replace("\"", ""),
                  "username" -> username,
                  "password" -> password,
                  "role" -> role,
                )))

           // save user to repository
           utils.AutoSignUp.save_user(userService, authTokenService, avatarService, credentialsProvider, authInfoRepository, passwordHasherRegistry, user_info)
          
            
//           var command = "echo '" + password.replace("$", "\\$") + "' | su - " + username
//           var r = Process(Seq("bash", "-c", command)).!
//           if (r == 0) {
             // Link itself as TACC account
             val tacc_info: JsValue = Json.obj(
                "firstName" -> (Json.parse(response) \ "result" \ "first_name").as[String].replace("\"", ""),
                "lastName" -> (Json.parse(response) \ "result" \ "last_name").as[String].replace("\"", ""),
                "username" -> username,
                "password" -> password
                )
             val tacc = new models.auth.TaccCredential(tacc_info)
             tacc.setToken(access_token)
             utils.AccountAllocator.map(username, tacc)
//           }
          }
        } 

        Thread.sleep(100)

        val credentials = Credentials(data.username, data.password)
        credentialsProvider.authenticate(credentials).flatMap { loginInfo =>
          val result = Redirect(routes.HomeController.use_cases())
          userService.retrieve(loginInfo).flatMap {
            case Some(user) =>
              // Allocate a training account for regular user
              if (!utils.AccountAllocator.contains(data.username)) {
                val tacc = utils.AccountAllocator.allocateTacc
                utils.AccountAllocator.map(data.username, tacc)
              }
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
   *  Handle user signin from Google
   */
  def googleSubmit(idTokenString: String) = silhouette.UnsecuredAction.async { implicit request: Request[AnyContent] =>
    
    val lines = Source.fromFile(configuration.underlying.getString("created.user.path")).getLines.toArray

val verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport, JacksonFactory.getDefaultInstance)
      .setAudience(List("496377681477-ebhtjhotd0nfsjt1rp358u4i59osfms0.apps.googleusercontent.com").asJavaCollection)
      .build();

    
    
  // (Receive idTokenString by HTTPS POST)
    val idToken = verifier.verify(idTokenString)
      val payload = idToken.getPayload()
    
      // Print user identifier
      val userId = payload.getSubject();
    
      // Get profile information from payload
      val email = payload.getEmail();
      val name = payload.get("name").toString();
      val familyName = payload.get("family_name").toString();
      val givenName = payload.get("given_name").toString();

      // Use or store profile information
      // ...
    
 

//    // check email for same user
    var password = ""

    if (lines.indexOf(email) != -1) {
      // user already exist
      password = lines(lines.indexOf(email) + 1)
    } else {
//       create a user for this email
      password = scala.util.Random.alphanumeric.take(10).mkString
      val writer = new BufferedWriter(new FileWriter(configuration.underlying.getString("created.user.path"), true))

      writer.write(email + "\n")
      writer.write(password + "\n")
      writer.close()

      val user_info: JsValue = Json.obj(
        "users" -> Json.arr(
          Json.obj(
            "firstName" -> givenName,
            "lastName" -> familyName,
            "username" -> email,
            "password" -> password,
            "role" -> "UserRole",
          )))

       utils.AutoSignUp.save_user(userService, authTokenService, avatarService, credentialsProvider, authInfoRepository, passwordHasherRegistry, user_info)
 
    }

    Thread.sleep(100)
    val credentials = Credentials(email, password)
    credentialsProvider.authenticate(credentials).flatMap { loginInfo =>

      val result = Redirect(routes.HomeController.home())
      userService.retrieve(loginInfo).flatMap {
        case Some(user) =>
          // link to a TACC account
          val tacc = utils.AccountAllocator.allocateTacc
          utils.AccountAllocator.map(email, tacc)
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
  
//  /**
//   *  Handle user signin from facebook
//   */
//  def facebookLogin(response: String, accessToken: String) = silhouette.UnsecuredAction.async { implicit request: Request[AnyContent] =>
//    val lines = Source.fromFile(configuration.underlying.getString("created.user.path")).getLines.toArray
//
//    // check email for same user
//    val email = (Json.parse(response) \ "username").as[String].replace("\"", "")
//    var password = ""
//
//    if (lines.indexOf(email) != -1) {
//      // user already exist
//      password = lines(lines.indexOf(email) + 1)
//    } else {
//      // create a user for this email
//      password = scala.util.Random.alphanumeric.take(10).mkString
//      val writer = new BufferedWriter(new FileWriter(configuration.underlying.getString("created.user.path"), true))
//
//      writer.write(email + "\n")
//      writer.write(password + "\n")
//      writer.close()
//
//      val user_info: JsValue = Json.obj(
//        "users" -> Json.arr(
//          Json.obj(
//            "firstName" -> (Json.parse(response) \ "name").as[String].replace("\"", "").split(" ")(0),
//            "lastName" -> (Json.parse(response) \ "name").as[String].replace("\"", "").split(" ")(1),
//            "username" -> email,
//            "password" -> password,
//            "role" -> "UserRole",
//          )))
//
//       utils.AutoSignUp.save_user(userService, authTokenService, avatarService, credentialsProvider, authInfoRepository, passwordHasherRegistry, user_info)
//    
//       // Link Facebook account
//           val fb_info: JsValue = Json.obj(
//             "firstName" -> (Json.parse(response) \ "name").as[String].replace("\"", "").split(" ")(0),
//             "lastName" -> (Json.parse(response) \ "name").as[String].replace("\"", "").split(" ")(1),
//             "username" -> email,
//           )
//           val fb = new models.auth.FacebookCredential(fb_info)
//           utils.AccountAllocator.map(email, fb)
//    }
//
//    Thread.sleep(100)
//    val credentials = Credentials(email, password)
//    credentialsProvider.authenticate(credentials).flatMap { loginInfo =>
//
//      val result = Redirect(routes.HomeController.home())
//      userService.retrieve(loginInfo).flatMap {
//        case Some(user) =>
//          // link to a TACC account
//          val tacc = utils.AccountAllocator.allocateTacc
//          utils.AccountAllocator.map(email, tacc)
//          val c = configuration.underlying
//          silhouette.env.authenticatorService.create(loginInfo).map {
//            case authenticator => authenticator
//          }.flatMap { authenticator =>
//            silhouette.env.eventBus.publish(LoginEvent(user, request))
//            silhouette.env.authenticatorService.init(authenticator).flatMap { v =>
//              silhouette.env.authenticatorService.embed(v, result)
//            }
//          }
//        case None => Future.failed(new IdentityNotFoundException("Couldn't find user"))
//      }
//    }.recover {
//      case _: ProviderException =>
//        Redirect(routes.SignInController.view()).flashing("error" -> Messages("invalid.credentials"))
//    }
//  }
}
