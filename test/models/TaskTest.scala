package models

import scala.slick.driver.H2Driver.simple._
import scala.slick.jdbc.meta._

import org.scalatest.FlatSpec
import org.scalatest.BeforeAndAfter

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
    Tables.Tasks.add(Task(Some(999L), "Peter"))
    val tasks = Tables.Tasks.findAll
    assert(1 == tasks.size)
  }

  it should "can be searched by id" in {
    createSchema()
    val task = Tables.Tasks.add(Task(None, "My is 55"))
    val tasks = Tables.Tasks.findById(task.id.get)
    assert(tasks.isDefined)
    assert(tasks.get.description.contains("is 55"))
  }
  
  it should "update completed time when marked complete" in {
    createSchema()
    val task = Task(None, "Task 10")
    val addedTask = Tables.Tasks.add(task)
    val completedTask = Tables.Tasks.completeTask(addedTask)
    assert(completedTask.id === addedTask.id)
    assert(completedTask.completedDate.isDefined)
    val dbTask = Tables.Tasks.findById(addedTask.id.get)
    assert(dbTask.isDefined)
    assert(dbTask.get.completedDate.isDefined)
    assert(dbTask.get.completedDate === completedTask.completedDate )
  }
}