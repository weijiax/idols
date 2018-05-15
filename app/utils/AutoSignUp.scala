package utils

import javax.inject._
import java.util.UUID
import play.api.libs.json._

import models.auth.User
import models.auth.Roles._

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.util.PasswordHasherRegistry
import com.mohiva.play.silhouette.api.services.AvatarService
import models.services.{ AuthTokenService, UserService }
import com.mohiva.play.silhouette.impl.providers._
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository

import scala.concurrent.{ ExecutionContext, Future }

import scala.concurrent.ExecutionContext.Implicits.global

case class AutoSignUp(
  userService: UserService,
  authTokenService: AuthTokenService,
  avatarService: AvatarService,
  credentialsProvider: CredentialsProvider,
  authInfoRepository: AuthInfoRepository,
  passwordHasherRegistry: PasswordHasherRegistry) {

  def save_user(data: JsValue) {
    println("saving!!")

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

        //        accessToken = Some(""),

        accessToken = if ((data \ "users" \ index \ "access_token").isInstanceOf[JsDefined]) Some((data \ "users" \ index \ "access_token").as[String].replace("\"", "")) else None,

        role = (data \ "users" \ index \ "role").as[String] match {
          case "UserRole" => UserRole
          case "AdminRole" => AdminRole
        },

        avatarURL = None,
        activated = true)

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
    println("saved!!")

  }

}

