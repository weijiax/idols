package models.auth

import utils.auth.DefaultEnv

import com.mohiva.play.silhouette.api.{ Authenticator, Authorization }
import play.api.mvc.Request
import scala.concurrent.Future

object Roles {
  sealed abstract class Role(val name: String)
  case object AdminRole extends Role("admin")
  case object UserRole extends Role("user")
}

case class WithRole(role: Roles.Role) extends Authorization[User, DefaultEnv#A] {
  override def isAuthorized[B](user: User, authenticator: DefaultEnv#A)(implicit request: Request[B]): Future[Boolean] =
    Future.successful(user.role == role)
}