package models

import org.joda.time.DateTime
import java.sql.Date

object Tables extends Tables {
  val profile = scala.slick.driver.MySQLDriver
}

trait Tables {

  val profile: scala.slick.driver.JdbcProfile
  import profile.simple._

  class Passwords(tag: Tag) extends Table[(String, String)](tag, "ACCOUNT_PRIVATE") {
    def username = column[String]("USERNAME")
    def password = column[String]("PASSWORD")
    def usernameFK = foreignKey("account_private_ibfk_1", username, Accounts)(_.username)

    def * = (username, password)
  }
  lazy val Passwords = new TableQuery(new Passwords(_));

  class Accounts(tag: Tag) extends Table[Account](tag, "ACCOUNT") {
    def username = column[String]("USERNAME", O.PrimaryKey)
    def firstName = column[String]("FIRSTNAME")
    def lastName = column[String]("LASTNAME")
    def email = column[String]("EMAIL")

    def * = (username, firstName, lastName, email) <> (Account.tupled, Account.unapply)
  }

  lazy val Accounts = new TableQuery(new Accounts(_)) {
    def findByUsername(username: String)(implicit session: Session): Option[Account] = {
      this.filter(_.username === username).firstOption
    }

    def createAccount(account: Account, password: String)(implicit session: Session): Account = {
      this += account
      Passwords += (account.username, password)
      account
    }

    def login(username: String, password: String)(implicit session: Session): Boolean = {
      val (u1, p1) = Passwords.filter(_.username === username).firstOption.getOrElse(false)
      p1 == password
    }
  }

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
    def noSteps = column[Int]("NO_STEPS")
    def stepsCompleted = column[Int]("STEPS_DONE")
    def repeatNoDays = column[Int]("NO_DAYS_REPEAT")
    def * = (id.?, desc, owner, startDate, completedDate, nextDate, dailyFlag, noSteps, stepsCompleted, repeatNoDays) <> (Task.tupled, Task.unapply)
  }

  lazy val Tasks = new TableQuery(new Tasks(_)) {

    def findAll(username: String)(implicit session: Session): List[Task] = {
      this.list.filter(
        task => task.owner == username && ((task.completedDate == None && new DateTime().isAfter(task.startDate)) ||
          (task.dailyFlag && task.nextDate.get.isBefore(new DateTime()))))
    }

    def findById(id: Long)(implicit session: Session): Option[Task] = {
      this.filter(_.id === id).firstOption
    }

    def add(task: Task)(implicit session: Session): Task = {
      val id = this returning this.map(_.id) += task
      task.copy(id = Some(id))
    }

    @throws(classOf[Error])
    def completeTask(id: Long)(implicit session: Session): DateTime =
      {
        val completedTime = new DateTime()
        val task: Task = findById(id).getOrElse(throw new Error("Unknown task"))
        val q = for { c <- this if c.id === id } yield (c.completedDate, c.nextDate, c.stepsCompleted)
        // not accumulated
        if (task.dailyFlag)
          q.update((Some(completedTime), Some(completedTime.plusDays(1)), 0))
        // future - accumulating
        /* else if (task.dailyFlag && accumulating) q.update((Some(completedTime.getMillis()), Some(comp */
        else
          q.update((Some(completedTime), None, 0))
        completedTime
      }

    def delete(id: Long)(implicit session: Session): Int = {
      val q = for { c <- this if c.id === id } yield c
      q.delete
    }

    def updateProgress(id: Long, step: Int)(implicit session: Session) = {
      val task: Task = findById(id).getOrElse(throw new Error("Unknown task"))
      if (step == task.noSteps) completeTask(id)
      else {
        val q = for { c <- this if c.id === id } yield (c.stepsCompleted)
        q.update((step))
      }
    }
  }
}

