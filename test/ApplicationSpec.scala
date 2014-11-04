import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner.RunWith
import play.api.test._
import play.api.test.Helpers._
import org.specs2.execute.AsResult
import org.specs2.execute.Result
import models.Tables._
import models.Task
import org.joda.time.DateTime
import play.api.db.slick.DB
import scala.slick.driver.H2Driver.simple.Session
import play.api.libs.json.JsArray
import org.omg.CosNaming.NamingContextPackage.NotFound
import play.api.libs.json.JsNull

/**
 * Add your spec here.
 * You can mock out a whole application including requests, plugins etc.
 * For more information, consult the wiki.
 */
@RunWith(classOf[JUnitRunner])
class ApplicationSpec extends Specification {
  val map_h2_settings = Map("IGNORECASE" -> "TRUE",
    "MODE" -> "MYSQL",
    "TRACE_LEVEL_FILE" -> "4")
  val configurationMap = inMemoryDatabase(options = map_h2_settings) + ("evolutionplugin" -> "enabled") // + ("db.default.slick.driver" -> "h2")
  def fakeApplication = FakeApplication(additionalConfiguration = configurationMap);

  abstract class InMemoryDBApplication extends WithApplication(fakeApplication)

  abstract class WithDbData extends WithApplication(fakeApplication) {
    override def around[T: AsResult](t: => T): Result = super.around {
      setupData()
      t
    }

    def setupData() {
      DB.withSession {
        implicit session: Session =>

          Tasks.add(new Task(description = "TestData1", owner = "nobody", startDate = new DateTime))
          Tasks.add(new Task(description = "TestData2", owner = "somebody", startDate = new DateTime))
      }
    }
  }

  "Application" should {

    "send 404 on a bad request" in new InMemoryDBApplication {
      route(FakeRequest(GET, "/boum")) must beNone
    }

    "render the index page" in new InMemoryDBApplication {
      val home = route(FakeRequest(GET, "/")).get

      status(home) must equalTo(OK)
      contentType(home) must beSome.which(_ == "text/html")
      //contentAsString(home) must contain ("Your new application is ready.")
    }

    "render the tasks page" in new InMemoryDBApplication {
      val tasks = route(FakeRequest(GET, "/tasks")).get

      status(tasks) must equalTo(OK)
      contentType(tasks) must beSome.which(_ == "text/html")
      contentAsString(tasks) must contain("All tasks")
    }

    "get list of tasks in json" in new WithDbData {

      val tasksJson = route(FakeRequest(GET, "/tasks.json")).get

      status(tasksJson) must equalTo(OK)
      contentType(tasksJson) must beSome.which(_ == "application/json")
      val json = contentAsJson(tasksJson)
      json must beAnInstanceOf[JsArray]
      json.as[JsArray].value must be size (2)
    }

    "put on task completes task" in new WithDbData {
      val completeTask = route(FakeRequest(PUT, "/tasks/1")).get

      status(completeTask) must equalTo(OK)
    }

    "put on task that doesn't exist results in error" in new WithDbData {
      val completeTask = route(FakeRequest(PUT, "/tasks/999")).get

      status(completeTask) must equalTo(BAD_REQUEST)
    }

    "create task through json" in new InMemoryDBApplication {
      val task = route(FakeRequest(POST, "/tasks.json").withJsonBody(JsNull)).get

      status(task) must equalTo(NOT_IMPLEMENTED)
    }

    "create task" in new InMemoryDBApplication {
      val task = route(FakeRequest(POST,"/tasks")).get

      status(task) must equalTo(SEE_OTHER)
    }
  }
}
