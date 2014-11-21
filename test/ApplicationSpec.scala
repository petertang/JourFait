import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner.RunWith
import play.api.test._
import play.api.test.Helpers._
import org.specs2.execute.AsResult
import org.specs2.execute.Result
import models.Tables._
import models._
import org.joda.time.DateTime
import play.api.db.slick.DB
import scala.slick.driver.H2Driver.simple._
import play.api.libs.json.JsArray
import org.omg.CosNaming.NamingContextPackage.NotFound
import play.api.libs.json._
import play.api.libs.json.Json._
import org.specs2.matcher.JsonMatchers

/**
 * Add your spec here.
 * You can mock out a whole application including requests, plugins etc.
 * For more information, consult the wiki.
 */
@RunWith(classOf[JUnitRunner])
class ApplicationSpec extends Specification with JsonMatchers {
  val map_h2_settings = Map("IGNORECASE" -> "TRUE",
    "MODE" -> "MYSQL",
    "TRACE_LEVEL_FILE" -> "4")
  val configurationMap = inMemoryDatabase(options = map_h2_settings) + ("evolutionplugin" -> "enabled") + ("application.langs" -> "en") // + ("db.default.slick.driver" -> "h2")
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
          Accounts += Account("test", "test", "test", "test@test.com")
          Passwords += ("test", "test")
          Tasks += new Task(description = "TestData1", owner = "nobody", startDate = new DateTime, noSteps = 5)
          Tasks += new Task(description = "TestData2", owner = "somebody", startDate = new DateTime)
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
      val completeTask = route(FakeRequest(PUT, "/tasks/1/complete")).get

      status(completeTask) must equalTo(OK)
    }

    "put on task that doesn't exist results in error" in new WithDbData {
      val completeTask = route(FakeRequest(PUT, "/tasks/999/complete")).get

      status(completeTask) must equalTo(BAD_REQUEST)
    }

    "create task through json, null value" in new InMemoryDBApplication {
      val task = route(FakeRequest(POST, "/tasks.json").withJsonBody(JsNull)).get

      status(task) must equalTo(BAD_REQUEST)
    }

    "create task" in new InMemoryDBApplication {
      val task = route(FakeRequest(POST, "/tasks")).get

      status(task) must equalTo(SEE_OTHER)
    }

    "create task in json" in new InMemoryDBApplication {
      val js = toJson(Map("description" -> toJson("Testing"), "dailyFlag" -> toJson(false)))
      val task = route(FakeRequest(POST, "/tasks.json").withJsonBody(js)).get
      // make sure the value is defined
      contentAsString(task) must /("id" -> "\\d+\\.\\d".r)
      status(task) must equalTo(OK)
    }

    "create task in json with no description" in new InMemoryDBApplication {
      val js = toJson(Map("dailyFlag" -> toJson(false)))
      val task = route(FakeRequest(POST, "/tasks.json").withJsonBody(js)).get

      status(task) must equalTo(BAD_REQUEST)
    }

    "create task in json with no dailyFlag" in new InMemoryDBApplication {
      val js = toJson(Map("description" -> toJson("Testing")))
      val task = route(FakeRequest(POST, "/tasks.json").withJsonBody(js)).get

      status(task) must equalTo(BAD_REQUEST)
    }

    "create task in json with number steps defined" in new InMemoryDBApplication {
      val js = toJson(Map(
        "description" -> toJson("Testing"),
        "dailyFlag" -> toJson(false),
        "noSteps" -> toJson(5)))
      val task = route(FakeRequest(POST, "/tasks.json").withJsonBody(js)).get

      status(task) must equalTo(OK)
      contentAsString(task) must /("noSteps" -> 5.0)
    }

    "Put on task for step, if setting last step, the task is completed" in new WithDbData {
      val completeTask = route(FakeRequest(PUT, "/tasks/1/step5")).get

      status(completeTask) must equalTo(OK)
    }

    "Try to set progress for task to be less than 1" in new WithDbData {
      val completeTask = route(FakeRequest(PUT, "/tasks/1/step0")).get

      status(completeTask) must equalTo(BAD_REQUEST)
    }

    "Try to set progress for task to be greater than no of steps" in new WithDbData {
      val completeTask = route(FakeRequest(PUT, "/tasks/1/step6")).get

      status(completeTask) must equalTo(BAD_REQUEST)
    }

    "create task in json with days to repeat defined" in new InMemoryDBApplication {
      val js = toJson(Map(
        "description" -> toJson("Testing"),
        "dailyFlag" -> toJson(false),
        "noSteps" -> toJson(5),
        "repeatNoDays" -> toJson(3)))
      val task = route(FakeRequest(POST, "/tasks.json").withJsonBody(js)).get

      status(task) must equalTo(OK)
      contentAsString(task) must /("repeatNoDays" -> 3.0)
    }

    "create task in json with start date defined as milliseconds" in new InMemoryDBApplication {
      val js = toJson(Map(
        "description" -> toJson("Testing"),
        "dailyFlag" -> toJson(false),
        "noSteps" -> toJson(5),
        "repeatNoDays" -> toJson(3),
        "startDate" -> toJson(1249871294873L)))
      val task = route(FakeRequest(POST, "/tasks.json").withJsonBody(js)).get

      status(task) must equalTo(OK)
      contentAsString(task) must /("startDate" -> 1249871294873.0)
    }

    "create task in json with start date defined as html5 date input format" in new InMemoryDBApplication {
      val js = toJson(Map(
        "description" -> toJson("Testing"),
        "dailyFlag" -> toJson(false),
        "noSteps" -> toJson(5),
        "repeatNoDays" -> toJson(3),
        "startDate" -> toJson("2014-03-03T12:22:01.555Z")))
      val task = route(FakeRequest(POST, "/tasks.json").withJsonBody(js)).get

      status(task) must equalTo(OK)
      contentAsString(task) must /("startDate" -> new DateTime(2014, 3, 3, 12, 22, 1, 555).getMillis().toDouble)
    }

    "register an account with json with incomplete data" in new InMemoryDBApplication {
      val js = toJson(Map(
        "username" -> "petertang"))
      val account = route(FakeRequest(POST, "/accounts").withJsonBody(js)).get

      status(account) must equalTo(BAD_REQUEST)
    }

    "register an account with json with complete data" in new InMemoryDBApplication {
      val js = toJson(Map(
        "username" -> toJson("petertang"),
        "firstName" -> toJson("peter"),
        "lastName" -> toJson("tang"),
        "email" -> toJson("tangp.peter@gmail.com"),
        "password" -> toJson("password")))

      val account = route(FakeRequest(POST, "/accounts").withJsonBody(js)).get

      status(account) must equalTo(OK)
    }

    "register an account with json with invalid email" in new InMemoryDBApplication {
      val js = toJson(Map(
        "username" -> toJson("petertang"),
        "firstName" -> toJson("peter"),
        "lastName" -> toJson("tang"),
        "email" -> toJson("tangp.peter"),
        "password" -> toJson("password")))

      val account = route(FakeRequest(POST, "/accounts").withJsonBody(js)).get

      status(account) must equalTo(BAD_REQUEST)
    }

    "register page" in new InMemoryDBApplication {
      val registerPage = route(FakeRequest(GET, "/register")).get

      status(registerPage) must equalTo(OK)
    }

    "login attempt with wrong password" in new WithDbData {
      val js = toJson(Map(
        "username" -> toJson("test"),
        "password" -> toJson("badPassword")))
      val login = route(FakeRequest(POST, "/login").withJsonBody(js)).get

      status(login) must equalTo(BAD_REQUEST)
    }

    "login attempt - success" in new WithDbData {
      val js = toJson(Map(
        "username" -> toJson("test"),
        "password" -> toJson("test")))
      val login = route(FakeRequest(POST, "/login").withJsonBody(js)).get

      status(login) must equalTo(OK)
      cookies(login).get("com.jourfait.username") must not(equalTo(None))
    }

  }
}
