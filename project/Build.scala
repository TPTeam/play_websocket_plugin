import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  val appName         = "websocket_plugin"
  val appVersion      = "0.4.11"

  val appDependencies = Seq(
  )

  val main = play.Project(appName, appVersion, appDependencies).settings(
    scalacOptions ++= Seq(
        "-feature",
        "-language:reflectiveCalls",
        "-language:postfixOps",
        "-language:implicitConversions"): _*     
  )

}
