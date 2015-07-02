val myName = "websocket-plugin"
val myVersion = "0.1.38"
val myOrganization = "org.tpteam"

name := myName

version := myVersion

lazy val commonSettings = Seq(
  organization := myOrganization,
  version := myVersion,
  scalaVersion := "2.11.4"
)

lazy val root = (project in file("."))
  .enablePlugins(PlayScala)
  .settings(commonSettings: _*)

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache,
  ws
)