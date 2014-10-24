package models

import scala.slick.driver.H2Driver.simple._
import scala.slick.jdbc.meta._

import org.scalatest.FlatSpec
import org.scalatest.BeforeAndAfter
import org.joda.time.DateTime

class TaskTest extends FlatSpec with BeforeAndAfter {

  implicit var session: Session = _

  object Tables extends Tables {
    val profile = scala.slick.driver.H2Driver
  }

  def createSchema() = {
    Tables.Tasks.ddl.create
  }

  before {
    session = Database.forURL("jdbc:h2:mem:test1", driver = "org.h2.Driver").createSession()
  }

  after {
    session.close()
  }

  "A task" should "return 0 elements" in {
    createSchema()
    val tasks = Tables.Tasks.findAll
    assert(0 == tasks.size)
  }

  it should "increase in size after an add" in {
    createSchema()
    Tables.Tasks.add(Task(Some(999L), description = "Peter", owner = "petertang", startDate = new DateTime()))
    val tasks = Tables.Tasks.findAll
    assert(1 == tasks.size)
  }

  it should "can be searched by id" in {
    createSchema()
    val task = Tables.Tasks.add(Task(None, "My is 55", owner = "petertang", startDate = new DateTime()))
    val tasks = Tables.Tasks.findById(task.id.get)
    assert(tasks.isDefined)
    assert(tasks.get.description.contains("is 55"))
  }

  it should "update completed time when marked complete" in {
    createSchema()
    val task = Task(None, "Task 10", owner = "petertang", startDate = new DateTime())
    val addedTask = Tables.Tasks.add(task)
    val completedTime = Tables.Tasks.completeTask(addedTask.id.get)
    val dbTask = Tables.Tasks.findById(addedTask.id.get)
    assert(dbTask.isDefined)
    assert(dbTask.get.completedDate.isDefined)
    assert(dbTask.get.completedDate.get === completedTime)
  }

  it should "update completed time and next time when marked complete and dailyflag is set" in {
    createSchema()
    val task = Task(None, "Task 10", owner = "petertang", dailyFlag = true, startDate = new DateTime())
    val addedTask = Tables.Tasks.add(task)
    val completedTime = Tables.Tasks.completeTask(addedTask.id.get)
    val dbTask = Tables.Tasks.findById(addedTask.id.get)
    assert(dbTask.isDefined)
    assert(dbTask.get.completedDate.isDefined)
    assert(dbTask.get.completedDate.get === completedTime)
    assert(dbTask.get.nextDate.isDefined)
  }

  it should "delete element if id found can be found" in {
    createSchema()
    val task = Tables.Tasks.add(Task(Some(999L), description = "Peter", owner = "petertang", startDate = new DateTime()))
    val tasks = Tables.Tasks.list
    val deleted = Tables.Tasks.delete(task.id.get)
    assert(deleted == 1)
    val tasksAfterDelete = Tables.Tasks.list
    assert(tasksAfterDelete.size == 0)
  }
}