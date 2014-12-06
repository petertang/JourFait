package controllers

import play.api.mvc.{ Flash, Action, Controller, Request, Result }
import play.api.data.Form
import play.api.data.Forms._
import models.Tables._
import models.Task
import play.api.db.slick._
import org.joda.time.DateTime
import play.api.libs.json.Json
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import org.omg.CosNaming.NamingContextPackage.NotFound
import play.api.libs.json.ConstraintReads
import play.api.mvc.Security
import controllers.helpers.Secured

object TasksController extends Controller with Secured {

  private val createTaskForm: Form[Task] = Form(
    mapping(
      "description" -> nonEmptyText,
      "dailyFlag" -> boolean)(formToTask)(taskToForm))

  val Html5IsoJodaDateReads = jodaDateReads("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
  implicit val taskWrites: Writes[Task] = Json.writes[Task]
  implicit val taskReads: Reads[Task] = (
    (JsPath \ "description").read[String](minLength[String](1)) and
    (JsPath \ "dailyFlag").read[Boolean] and
    (JsPath \ "noSteps").readNullable[Int] and
    (JsPath \ "startDate").readNullable[DateTime](Html5IsoJodaDateReads) and
    (JsPath \ "repeatNoDays").readNullable[Int])(jsonTaskToTask _)

  private def formToTask(description: String, dailyFlag: Boolean) = {
    Task(None, description, owner = "petertang", startDate = new DateTime(), dailyFlag = dailyFlag)
  }

  // TODO: Figure out how to avoid putting in defaults here again -- error prone, two places needed to change default
  private def jsonTaskToTask(description: String, dailyFlag: Boolean, noSteps: Option[Int], startDate: Option[DateTime], repeatNoDays: Option[Int]): Task = {
    Task(None, description, owner = "petertang", startDate = startDate.getOrElse(new DateTime()), dailyFlag = dailyFlag, noSteps = noSteps.getOrElse(1), repeatNoDays = repeatNoDays.getOrElse(1))
  }

  private def taskToForm(task: Task) = {
    Option(task.description, task.dailyFlag)
  }

  def list = Action {
    implicit request =>
      Ok(views.html.tasks())
  }

  def listJson = DBAction {
    implicit rs =>
      rs.request.session.get("username").fold[Result](Unauthorized)(username => {
          val tasks = Tasks.findAll(username) 
          Ok(Json.toJson(tasks))
      })
  }

  def show(id: Long) = DBAction {
    implicit rs =>
      Tasks.findById(id).map {
        task => Ok(views.html.task(task))
      }.getOrElse(NotFound)
  }

  def save() = DBAction {
    implicit rs =>

      val newTaskForm = createTaskForm.bindFromRequest()

      newTaskForm.fold(
        hasErrors = {
          form =>
            Redirect(routes.TasksController.newTask).flashing(Flash(form.data) + ("error" -> "myerror"))
        },
        success = {
          newTask =>
            Tasks.add(newTask)
            Redirect(routes.TasksController.list)
        })
  }

  def newTask = Action {
    implicit request =>
      val form = if (request2flash.get("error").isDefined) {
        createTaskForm.bind(request2flash.data)
      } else
        createTaskForm

      Ok(views.html.editTask(form))
  }

  def complete(id: Long) = DBAction {
    implicit rs =>
      try {
        Tasks.completeTask(id)
        Ok
      } catch {
        case error: Error => BadRequest
      }
  }

  def progress(id: Long, step: Int) = DBAction {
    implicit rs =>
      Tasks.findById(id) match {
        case None => BadRequest
        case Some(task) => {
          if (step < 1 || step > task.noSteps) BadRequest
          else {
            Tasks.updateProgress(id, step)
            Ok
          }
        }
      }
  }

  def delete(id: Long) = DBAction {
    implicit rs =>
      if (Tasks.delete(id) == 0) NotImplemented
      else Ok
  }

  def saveJson = DBAction(parse.json) {
    // TODO: Create trait/function to allow for "Authorized" decorator    
    implicit rs =>
      val json = rs.body
      rs.request.session.get("username").fold[Result](Unauthorized)(username =>
        json.validate[Task].fold(
          valid = {
            task =>
              // for now manually update username here
              val newTask = Tasks.add(task.copy(owner = username))
              Ok(Json.toJson(newTask))
          },
          invalid = {
            errors => BadRequest(JsError.toFlatJson(errors))
          }))
  }

} 
 