package controllers

import play.api.mvc.{ Flash, Action, Controller }
import play.api.data.Form
import play.api.data.Forms._
import models.Tables._
import models.Task;
import play.api.db.slick._

object TasksController extends Controller {

  private val taskForm: Form[Task] = Form(
    mapping(
      "id" -> longNumber.verifying("validation.id.duplicate", _ == 1),
      "description" -> nonEmptyText,
      "date" -> optional(longNumber),
      "dailyFlag" -> boolean)(Task.apply)(Task.unapply))

  def list = DBAction {
    implicit rs =>
      val tasks = Tasks.findAll

      Ok(views.html.tasks(tasks))
  }

  def show(id: Long) = DBAction {
    implicit rs =>
      Tasks.findById(id).map {
        task => Ok(views.html.task(task))
      }.getOrElse(NotFound)
  }

  def save() = DBAction {
    implicit rs =>
      val newTaskForm = taskForm.bindFromRequest()

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
        System.out.println("Binding form...")
        taskForm.bind(request2flash.data)
      } else
        taskForm

      Ok(views.html.editTask(form))
  }

} 
 