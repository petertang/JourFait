package controllers

import play.api.mvc.{ Flash, Action, Controller }
import play.api.data.Form
import play.api.data.Forms._
import models.Tables._
import models.Task;
import play.api.db.slick._
import org.joda.time.DateTime
import play.api.libs.json.Json
import play.api.libs.json._
import play.api.libs.functional.syntax._

object TasksController extends Controller {

  private val createTaskForm: Form[Task] = Form(
    mapping(
      "description" -> nonEmptyText,
      "dailyFlag" -> boolean)(formToTask)(taskToForm))

  implicit val taskWrites: Writes[Task] = Json.writes[Task]

  private def formToTask(description: String, dailyFlag: Boolean) = {
    Task(None, description, owner = "petertang", startDate = new DateTime(), dailyFlag = dailyFlag)
  }

  private def taskToForm(task: Task) = {
    Option(task.description, task.dailyFlag)
  }

  def list = DBAction {
    implicit rs =>
      val tasks = Tasks.findAll

      Ok(views.html.tasks(tasks, createTaskForm))
  }

  def listJson = DBAction {
    implicit rs =>
      val tasks = Tasks.findAll

      Ok(Json.toJson(tasks))
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
      System.out.println(request2flash)
      val form = if (request2flash.get("error").isDefined) {
        createTaskForm.bind(request2flash.data)
      } else
        createTaskForm

      Ok(views.html.editTask(form))
  }

  def complete(id: Long) = DBAction {
    implicit rs =>
      Tasks.completeTask(id)
      Ok
  }

  def progress(id: Long, step: Int) = Action {
    Ok
  }

  def delete(id: Long) = DBAction {
    implicit rs =>
      if (Tasks.delete(id) == 0) NotImplemented
      else Ok
  }
} 
 