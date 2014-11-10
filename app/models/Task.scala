package models

import org.joda.time.DateTime

case class Task(
    id: Option[Long] = None, 
    description: String, 
    owner: String, 
    startDate: DateTime, 
    completedDate: Option[DateTime] = None, 
    nextDate: Option[DateTime] = None, 
    dailyFlag: Boolean = false, 
    noSteps: Int = 1, 
    stepsCompleted: Int = 0)

/*
object Task {

  import com.github.nscala_time.time.Imports._

  var tasks = Set(
    Task(1L, "Finish homework", Some(new DateTime(2014, 9, 10, 0, 0).getMillis())),
    Task(2L, "Duolingo.com"),
    Task(3L, "French reading"),
    Task(4L, "French listening"),
    Task(5L, "French speaking"),
    Task(6L, "French writing"),
    Task(7L, "Scala book"),
    Task(8L, "100 pushups"))

  
}
*/