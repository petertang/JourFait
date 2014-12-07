package models

import org.joda.time.DateTime
import java.sql.Date
import java.security.SecureRandom
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.codec.binary.Hex

object Tables extends Tables {
  val profile = scala.slick.driver.MySQLDriver
}

trait Tables {

  val profile: scala.slick.driver.JdbcProfile
  import profile.simple._

  implicit lazy val jodaToSqlDate = MappedColumnType.base[DateTime, Date](
    { jd => if (jd == null) null else new Date(jd.getMillis()) },
    { d => if (d == null) null else new DateTime(d.getTime()) })

  class Passwords(tag: Tag) extends Table[(String, String, String)](tag, "ACCOUNT_PRIVATE") {
    def username = column[String]("USERNAME")
    def password = column[String]("PASSWORD")
    def salt = column[String]("SALT")
    def usernameFK = foreignKey("account_private_ibfk_1", username, Accounts)(_.username)

    def * = (username, password, salt)
  }
  lazy val Passwords = new TableQuery(new Passwords(_));

  class AccountVerification(tag: Tag) extends Table[(String, String, DateTime)](tag, "ACCOUNT_EMAIL_VERIFY") {
    def username = column[String]("USERNAME")
    def verifyToken = column[String]("EMAIL_TOKEN")
    def expiryDate = column[DateTime]("EXPIRY_DATE")

    def * = (username, verifyToken, expiryDate)
  }
  lazy val AccountVerificationTokens = new TableQuery(new AccountVerification(_));

  class Accounts(tag: Tag) extends Table[Account](tag, "ACCOUNT") {
    def username = column[String]("USERNAME", O.PrimaryKey)
    def firstName = column[String]("FIRSTNAME")
    def lastName = column[String]("LASTNAME")
    def email = column[String]("EMAIL")
    def verified = column[Boolean]("VERIFIED")
    def expiryDate = column[Option[DateTime]]("EXPIRY_DATE")

    def * = (username, firstName, lastName, email, verified, expiryDate) <> (Account.tupled, Account.unapply)
  }

  lazy val Accounts = new TableQuery(new Accounts(_)) {
    val SALT_BYTE_SIZE: Int = 24;
    val VERIFY_GRACE_PERIOD: Int = 3;

    private def getPassword(username: String)(implicit session: Session) = {
      Passwords.filter(_.username === username).first
    }

    def findByUsername(username: String)(implicit session: Session): Option[Account] = {
      this.filter(_.username === username).firstOption
    }

    def createAccount(account: Account, password: String)(implicit session: Session): Account = {
      this += account.copy(expiryDate = Some(new DateTime().plusDays(VERIFY_GRACE_PERIOD)))
      val random: SecureRandom = new SecureRandom();
      val salt: Array[Byte] = new Array[Byte](SALT_BYTE_SIZE);
      random.nextBytes(salt);
      val saltString: String = Hex.encodeHexString(salt)
      val saltedHashedPassword: String = DigestUtils.sha256Hex(saltString + password)
      Passwords += (account.username, saltedHashedPassword, saltString)
      account
    }

    def login(username: String, password: String)(implicit session: Session): Option[Account] = {
      val userAndPassword = (for (
        a <- this if a.username === username;
        p <- Passwords if p.username === a.username
      ) yield (a, p)).firstOption

      userAndPassword.flatMap {
        case (user, (username, dbPass, salt)) => {
          if ((user.expiryDate.isEmpty || user.expiryDate.get.isAfter(new DateTime)) &&
            dbPass == DigestUtils.sha256Hex(salt + password))
            Some(user)
          else
            None
        }
      }
    }

    def createToken(account: Account)(implicit session: Session): String = {
      val q = for (accountVerification <- AccountVerificationTokens if accountVerification.username === account.username) yield (accountVerification)
      q.delete
      val token = account.username + new DateTime()
      AccountVerificationTokens += (account.username, token, new DateTime().plusDays(VERIFY_GRACE_PERIOD))
      token
    }

    def verifyAccount(username: String, token: String)(implicit session: Session): Boolean = {
      if (AccountVerificationTokens.filter(
        accountVerification => (
          accountVerification.username === username &&
          accountVerification.verifyToken === token &&
          accountVerification.expiryDate > new DateTime())).firstOption.isDefined) {
        // set account to verified
        val q = for (account <- this if account.username === username) yield (account.verified, account.expiryDate)
        q.update((true, None))
        true
      } else {
        false
      }

    }
  }

  class Tasks(tag: Tag) extends Table[Task](tag, "TASK") {

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
        if (task.dailyFlag) {
          val dateToAddTo = if (task.repeatNoDays == 1) completedTime else task.nextDate.getOrElse(task.startDate)
          q.update((Some(completedTime), Some(dateToAddTo.plusDays(task.repeatNoDays)), 0))
          // future - accumulating
          /* else if (task.dailyFlag && accumulating) q.update((Some(completedTime.getMillis()), Some(comp */
        } else {
          q.update((Some(completedTime), None, 0))
        }
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

