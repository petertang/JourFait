package models

import org.joda.time.DateTime
import java.sql.Date

object Tables extends Tables {
  val profile = scala.slick.driver.MySQLDriver
}

trait Tables {

  val profile: scala.slick.driver.JdbcProfile

  import profile.simple._

  class Tasks(tag: Tag) extends Table[Task](tag, "TASK") {

    implicit val jodaToSqlDate = MappedColumnType.base[DateTime, Date](
      { jd => if (jd == null) null else new Date(jd.getMillis()) },
      { d => if (d == null) null else new DateTime(d.getTime()) })

    def id = column[Long]("ID", O.PrimaryKey, O.AutoInc)
    def desc = column[String]("DESCRIPTION")
    def owner = column[String]("OWNER")
    def startDate = column[DateTime]("START_DATE")
    def completedDate = column[Option[DateTime]]("COMPLETED_DATE")
    def nextDate = column[Option[DateTime]]("NEXT_DATE")
    def dailyFlag = column[Boolean]("DAILY_FLAG")
    def * = (id.?, desc, owner, startDate, completedDate, nextDate, dailyFlag) <> (Task.tupled, Task.unapply)
  }

  lazy val Tasks = new TableQuery(new Tasks(_)) {

    def findAll(implicit session: Session): List[Task] = this.list.filter(task => task.completedDate == None || (task.dailyFlag && task.nextDate.get.getMillis() < new DateTime().getMillis()))

    def findById(id: Long)(implicit session: Session): Option[Task] = {
      this.filter(_.id === id).firstOption
    }

    def add(task: Task)(implicit session: Session): Task = {
      val id = this returning this.map(_.id) += task
      task.copy(id = Some(id))
    }

    def completeTask(id: Long)(implicit session: Session): DateTime =
      {
        val completedTime = new DateTime()
        val task: Task = findById(id).get
        val q = for { c <- this if c.id === id } yield (c.completedDate, c.nextDate)
        // not accumulated
        if (task.dailyFlag)
          q.update((Some(completedTime), Some(completedTime.plusDays(1))))
        // future - accumulating
        /* else if (task.dailyFlag && accumulating) q.update((Some(completedTime.getMillis()), Some(comp */
        else
          q.update((Some(completedTime), None))
        completedTime
      }
    
    def delete(id: Long)(implicit session: Session): Int = {
      val q = for { c <- this if c.id === id } yield c
      q.delete
    }
  }
}

