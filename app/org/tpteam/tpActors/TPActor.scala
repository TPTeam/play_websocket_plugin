package org.tpteam.tpActors

import akka.actor.{Cancellable, Actor}
import org.tpteam.tpActors.WSClientInnerMsgs.{Quit, Pong, Ping}
import play.api.libs.iteratee.Concurrent
import play.api.libs.json.{Json, JsValue}
import play.api.libs.concurrent.Execution.Implicits._


import scala.concurrent.duration.FiniteDuration

abstract class TPActor extends Actor with JsPushee {

  // must be set in application.conf
  val application_config = play.Play.application.configuration

  // timeout for starting after InitDone: this influences only the first actor's timeout
  lazy val initTimeout: FiniteDuration =
    try {
      new FiniteDuration(application_config.getConfig("websockets").getLong("initTimeout"), scala.concurrent.duration.MILLISECONDS)
    } catch {
      case _: Throwable =>
        browserTimeout * 2
    }

  // browser timeout: the actor wil kill himself if doesn't receive any ping from the browser.
  lazy val browserTimeout: FiniteDuration = new FiniteDuration(
    try {
      application_config.getConfig("websockets").getLong("browserTimeout")
    } catch {
      case _: Throwable =>
        2000
    }, scala.concurrent.duration.MILLISECONDS)

  // browser timeout: the actor wil kill himself if doesn't receive any ping from the browser
  lazy val pingTimeout: FiniteDuration =
    try {
      new FiniteDuration(application_config.getConfig("websockets").getLong("pingTimeout"), scala.concurrent.duration.MILLISECONDS)
    } catch {
      case _: Throwable =>
        browserTimeout / 3
    }

  def pingReceive(implicit channel: Concurrent.Channel[JsValue]): PartialFunction[Any, Unit] = {
    case Ping =>
      --->(Json.obj(
        "ping" -> true,
        "pingTimeout" -> pingTimeout.toMillis,
        "nextTimeout" -> browserTimeout.toMillis
      ))
  }

  def pongReceive(nextStop: Cancellable)(implicit channel: Concurrent.Channel[JsValue]): PartialFunction[Any, Unit] = {
    case Pong =>
      nextStop.cancel()
      rescheduleOperative(browserTimeout)
  }

  def rescheduleOperative(timeout: FiniteDuration)(implicit channel: Concurrent.Channel[JsValue]): Unit = {
    val newNextStop = context.system.scheduler.scheduleOnce(browserTimeout, self, Quit)
    context.become(operativePingPong(newNextStop), discardOld = true)
    context.system.scheduler.scheduleOnce(pingTimeout, self, Ping)
  }

  def operativePingPong(nextStop: Cancellable)(implicit channel: Concurrent.Channel[JsValue]): PartialFunction[Any, Unit]

}