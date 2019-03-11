package controllers

import play.api._
import play.api.mvc._
import javax.inject._
import java.io._
import javax.imageio.ImageIO
import scala.concurrent.ExecutionContext;

@Singleton
class GetDynamicFileController @Inject() (cc: ControllerComponents)(implicit ec: ExecutionContext)
  extends AbstractController(cc) {

  def getFile(name: String) = Action { implicit request =>
    Ok.sendFile(
      new java.io.File("./public/DynamicFiles/" + name),
      inline = false
    )
  }
}