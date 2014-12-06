name := """DailyDo"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.1"

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache,
  ws,
  "com.github.nscala-time" %% "nscala-time" % "1.4.0",
  "org.scalatestplus" %% "play" % "1.2.0" % "test",
  "mysql" % "mysql-connector-java" % "5.1.27",
  "com.typesafe.slick" %% "slick" % "2.1.0",
  "com.typesafe.play" %% "play-slick" % "0.8.0",
  "org.apache.commons" % "commons-email" % "1.3.3"
)
