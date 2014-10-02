package models

object Tables extends Tables {
  val profile = scala.slick.driver.MySQLDriver
}

trait Tables {

  val profile: scala.slick.driver.JdbcProfile

  import profile.simple._

  class Tasks(tag: Tag) extends Table[Task](tag, "TASK") {
    def id = column[Long]("ID")
    def desc = column[String]("DESCRIPTION")
    def completedDate = column[Option[Long]]("COMPLETED_DATE")
    def dailyFlag = column[Boolean]("DAILY_FLAG")
    def * = (id, desc, completedDate, dailyFlag) <> (Task.tupled, Task.unapply)
  }

  lazy val Tasks = new TableQuery(new Tasks(_)) {

    def findAll(implicit session: Session): List[Task] = this.list

    def findById(id: Long)(implicit session: Session): Option[Task] = {
      this.filter(_.id === id).firstOption
    }

    def add(task: Task)(implicit session: Session): Unit = { this += task }

    //def completeTask(task: Task) = tasks - task + Task(task.id, task.description, Some(new DateTime().getMillis()))

  }
}
