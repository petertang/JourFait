package controllers

import play.api._
import play.api.mvc._

object Application extends Controller {

  def index = Action {
    implicit request =>
      request.session.get("username") match {
        case Some(_) => Redirect(routes.TasksController.list)
        case None => Ok(views.html.index())
      }
  }

  def register = Action {
    implicit request =>
      Ok(views.html.register())
  }
}