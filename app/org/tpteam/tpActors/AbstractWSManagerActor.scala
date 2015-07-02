package org.tpteam.tpActors

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import org.tpteam.tpActors.WSClientMsgs.JsFromClient
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.iteratee.{Concurrent, Enumerator, Iteratee}
import play.api.libs.json.JsValue
import play.api.mvc.RequestHeader

abstract class AbstractWSManagerActor extends Actor with ActorLogging {

  import WSInnerMsgs._

  def receive = connectionManagement orElse dispatch

  def dispatch: PartialFunction[Any, Unit] = {
    case msg =>
      context.children.foreach(act => act ! msg)
  }

  def clientProp(implicit request: RequestHeader, userSession: UserSessionData): Props

  def connectionManagement: PartialFunction[Any, Unit] = {

    case AuthenticatedConnect(r, u) =>

      implicit val _r: RequestHeader = r
      implicit val _u: UserSessionData = u

      import WSClientInnerMsgs._

      val act: ActorRef = context.actorOf(clientProp)

      val outChannel: Enumerator[JsValue] =
        Concurrent.unicast[JsValue](
          onStart =
            (c) => {
              act ! InitDone(c)
            },
          onComplete = () => act ! Quit,
          onError = { (_, _) => act ! Quit })

      val inChannel: Iteratee[JsValue, Unit] =
        Iteratee.foreach[JsValue](msg =>
          msg.\("pong").asOpt[Boolean] match {
            case Some(true) => act ! Pong
            case _ =>
              act ! JsFromClient(msg)
          })

      sender ! Connected(inChannel, outChannel)

    case Disconnect =>

      context.stop(sender)

  }

}