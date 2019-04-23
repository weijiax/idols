package controllers

import java.util.UUID
import javax.inject.Inject

import models.auth.Roles.UserRole

import com.mohiva.play.silhouette.api._
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.api.services.AvatarService
import com.mohiva.play.silhouette.api.util.PasswordHasherRegistry
import com.mohiva.play.silhouette.impl.providers._
import forms.RequestForm
import models.auth.User
import models.services.{ AuthTokenService, UserService }
import org.webjars.play.WebJarsUtil
import play.api.i18n.{ I18nSupport, Messages }
import play.api.libs.mailer.{ Email, MailerClient }
import play.api.mvc.{ AbstractController, AnyContent, ControllerComponents, Request }
import utils.auth.DefaultEnv
import utils.NotebookAllocator
import utils.AccountAllocator

import scala.concurrent.{ ExecutionContext, Future }

import java.io.FileWriter
import java.io.BufferedWriter
import java.io.PrintWriter

/**
 * The `Request` controller.
 *
 * @param components             The Play controller components.
 * @param silhouette             The Silhouette stack.
 * @param userService            The user service implementation.
 * @param authInfoRepository     The auth info repository implementation.
 * @param authTokenService       The auth token service implementation.
 * @param avatarService          The avatar service implementation.
 * @param passwordHasherRegistry The password hasher registry.
 * @param mailerClient           The mailer client.
 * @param webJarsUtil            The webjar util.
 * @param assets                 The Play assets finder.
 * @param ex                     The execution context.
 */
class RequestController @Inject() (
  components: ControllerComponents,
  silhouette: Silhouette[DefaultEnv],
  userService: UserService,
  authInfoRepository: AuthInfoRepository,
  authTokenService: AuthTokenService,
  avatarService: AvatarService,
  passwordHasherRegistry: PasswordHasherRegistry,
  mailerClient: MailerClient)(configuration: play.api.Configuration)(

  implicit
  webJarsUtil: WebJarsUtil,
  assets: AssetsFinder,
  ex: ExecutionContext) extends AbstractController(components) with I18nSupport {

  /**
   * Views the `Sign Up` page.
   *
   * @return The result to display.
   */
  def view = silhouette.UnsecuredAction.async { implicit request: Request[AnyContent] =>
    Future.successful(Ok(views.html.request(RequestForm.form)))
  }

  /**
   * Handles the submitted form.
   *
   * @return The result to display.
   */
  def submit = silhouette.UnsecuredAction.async {

    implicit request: Request[AnyContent] =>

      RequestForm.form.bindFromRequest.fold(
        form => Future.successful(BadRequest(views.html.request(form))),
        data => {
          val user = data.email
          //          var url = "stampede2.tacc.utexas.edu:"
          //          var password = ""
          //          if (NotebookAllocator.contains(user)) {
          //            val session = NotebookAllocator.get(user)
          //            url += session.port
          //            password = session.password
          //          } else {
          //
          //            if (NotebookAllocator.isEmpty) {
          //              Future.successful(Ok("Sorry, there is no more notebook instances available."))
          //            } else {
          //
          //              val writer = new BufferedWriter(new FileWriter(configuration.underlying.getString("jupyter.records"), true))
          //
          //              val session = NotebookAllocator.allocateNotebook
          //              NotebookAllocator.map(user, session)
          //              url += session.port
          //              password = session.password
          //              writer.write(data.firstName + "," + data.lastName + "," + data.email + "," + url + "\n")
          //              writer.close
          //            }
          //
          //          }
          //          Future.successful(Ok(views.html.notebookInfo(data.firstName, data.lastName, data.email, url, password)))

          val account = AccountAllocator.allocateTacc
          Future.successful(Ok(views.html.notebookInfo(data.firstName, data.lastName, data.email, account.username, account.password)))
        })
  }
}
