package org.tpteam.tpActors

import akka.actor.{ActorRef, Props}
import play.api.mvc.RequestHeader

abstract class StatefullWSManagerActor extends AbstractWSManagerActor {

  def wsDevice: ((RequestHeader, UserSessionData) => Props)

  def clientProp(implicit request: RequestHeader, userSession: UserSessionData): Props =
    Props(
      new WsDispatcherClientActor(wsDevice(request, userSession))
    )

}

class WsDispatcherClientActor(
                               deviceProp: Props)
                             (implicit request: RequestHeader, userSession: UserSessionData) extends AbstractWsClientActor {

  val device = context.actorOf(deviceProp)

  def operative(implicit request: RequestHeader, userSession: UserSessionData): ((ActorRef) => PartialFunction[Any, Unit]) = {
    (wsClient: ActorRef) => {
      case msg =>
        device ! msg
    }
  }

}
