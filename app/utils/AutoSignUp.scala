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

object AutoSignUp {

  def save_user(
    userService: UserService,
    authTokenService: AuthTokenService,
    avatarService: AvatarService,
    credentialsProvider: CredentialsProvider,
    authInfoRepository: AuthInfoRepository,
    passwordHasherRegistry: PasswordHasherRegistry, data: JsValue) {

    // loop through all users's info and create users
    var index = 0
    while ((data \ "users" \ index).isInstanceOf[JsDefined]) {
      var authInfo = passwordHasherRegistry.current.hash((data \ "users" \ index \ "password").as[String].replace("\"", ""))
      val loginInfo = LoginInfo(CredentialsProvider.ID, (data \ "users" \ index \ "username").as[String].replace("\"", ""))

      var user = User(
        userID = UUID.randomUUID(),
        loginInfo = loginInfo,
        firstName = Some((data \ "users" \ index \ "firstName").as[String].replace("\"", "")),
        lastName = Some((data \ "users" \ index \ "lastName").as[String].replace("\"", "")),
        fullName = Some((data \ "users" \ index \ "firstName").as[String].replace("\"", "") + " " + (data \ "users" \ index \ "lastName").as[String].replace("\"", "")),
        username = Some((data \ "users" \ index \ "username").as[String].replace("\"", "")),

        //        accessToken = if ((data \ "users" \ index \ "access_token").isInstanceOf[JsDefined]) Some((data \ "users" \ index \ "access_token").as[String].replace("\"", "")) else None,

        role = (data \ "users" \ index \ "role").as[String] match {
          case "UserRole" => UserRole
          case "AdminRole" => AdminRole
          case _ => UserRole
        },

        avatarURL = None,
        activated = true)

      //      utils.AccountAllocator.map(user.username.getOrElse(""), taccName)

      for {
        avatar <- avatarService.retrieveURL((data \ "users" \ index \ "username").as[String].replace("\"", ""))
        user <- userService.save(user.copy(avatarURL = avatar))
        authInfo <- authInfoRepository.add(loginInfo, authInfo)
        authToken <- authTokenService.create(user.userID)
      } yield {

      }
      index += 1
    }
  }

}

