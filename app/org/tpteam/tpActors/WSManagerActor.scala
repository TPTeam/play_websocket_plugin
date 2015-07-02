package org.tpteam.tpActors

import akka.actor.{ActorRef, Props}
import play.api.mvc.RequestHeader

abstract class WSManagerActor extends AbstractWSManagerActor {

  def operative(implicit request: RequestHeader, userSession: UserSessionData):
  ((ActorRef) => Receive)

  def clientProp(implicit request: RequestHeader, userSession: UserSessionData): Props =
    Props(
      new WsClientActor(operative)(request, userSession)
    )

}

class WsClientActor(
                     _operative: ((ActorRef) => PartialFunction[Any, Unit]))
                   (implicit request: RequestHeader, userSession: UserSessionData) extends AbstractWsClientActor {

  def operative(implicit request: RequestHeader, userSession: UserSessionData) = _operative

}
