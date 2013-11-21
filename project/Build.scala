import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  val appName         = "websocket_plugin"
  val appVersion      = "0.3"

  val appDependencies = Seq(
    // Add your project dependencies here,
    jdbc,
    anorm
  )

  val main = play.Project(appName, appVersion, appDependencies).settings(
    // Add your own project settings here      
  )

}
