package org.tpteam.tpActors

import akka.actor.{ActorRef, Cancellable}
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.iteratee.Concurrent
import play.api.libs.json.{JsValue, _}
import play.api.mvc.RequestHeader

import scala.concurrent.duration._

abstract class AbstractWsClientActor(implicit request: RequestHeader, userSession: UserSessionData) extends TPActor {

  def operative(implicit request: RequestHeader, userSession: UserSessionData): ((ActorRef) => PartialFunction[Any, Unit])

  import WSClientInnerMsgs._

  def receive = {

    case InitDone(channel) =>
      rescheduleOperative(initTimeout)(channel)

    case any =>
      context.system.scheduler.scheduleOnce(50 milliseconds)(
        self forward any)

  }

  def operativePingPong(nextStop: Cancellable)(implicit channel: Concurrent.Channel[JsValue]): PartialFunction[Any, Unit] = {
    pingReceive orElse
      pongReceive(nextStop) orElse
      quitReceive orElse
      jsToClientReceive orElse
      operative.apply(self)
  }

  import WSClientMsgs._

  def jsToClientReceive(implicit channel: Concurrent.Channel[JsValue]): PartialFunction[Any, Unit] = {
    case JsToClient(js) =>
      --->(js)
  }

  def quitReceive(implicit channel: Concurrent.Channel[JsValue]): PartialFunction[Any, Unit] = {
    case Quit =>
      channel.eofAndEnd()
      context.stop(self)
  }
}