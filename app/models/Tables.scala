package models

import org.joda.time.DateTime

object Tables extends Tables {
  val profile = scala.slick.driver.MySQLDriver
}

trait Tables {

  val profile: scala.slick.driver.JdbcProfile

  import profile.simple._

  class Tasks(tag: Tag) extends Table[Task](tag, "TASK") {
    def id = column[Long]("ID", O.PrimaryKey, O.AutoInc)
    def desc = column[String]("DESCRIPTION")
    def completedDate = column[Option[Long]]("COMPLETED_DATE")
    def dailyFlag = column[Boolean]("DAILY_FLAG")
    def * = (id.?, desc, completedDate, dailyFlag) <> (Task.tupled, Task.unapply)
  }

  lazy val Tasks = new TableQuery(new Tasks(_)) {

    def findAll(implicit session: Session): List[Task] = this.list

    def findById(id: Long)(implicit session: Session): Option[Task] = {
      this.filter(_.id === id).firstOption
    }

    def add(task: Task)(implicit session: Session): Task = {
      val id = this returning this.map(_.id) += task
      task.copy(Some(id))
    }
      

    def completeTask(task: Task)(implicit session: Session): Task =
      {
        val completedTime: Long = new DateTime().getMillis()
        val q = for { c <- this if c.id === task.id } yield c.completedDate
        q.update(Some(completedTime))
        task.copy(completedDate = Some(completedTime))
      }
  }
}

