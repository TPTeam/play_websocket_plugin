package org.tpteam.tpActors

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.concurrent._
import play.api.libs.iteratee._
import play.api.libs.json._
import play.api.mvc._

import scala.concurrent.duration._

abstract class WebSocketManager[M <: AbstractWSManagerActor](implicit ct: scala.reflect.ClassTag[M]) {

  val name: Option[String] = None

  private val _actor =
    name match {
      case Some(n) => Akka.system.actorOf(Props[M], n)
      case _ => Akka.system.actorOf(Props[M])
    }

  def actor: ActorRef = _actor

  // time to wait for the websocket upgrade?
  implicit val timeout = Timeout(30 seconds)

  import WSInnerMsgs._

  def control(implicit request: RequestHeader, userSession: UserSessionData): scala.concurrent.Future[(Iteratee[JsValue, _], Enumerator[JsValue])] = {

    (actor ? AuthenticatedConnect(request, userSession)).map {

      case Connected(iteratee, enumerator) =>
        (iteratee, enumerator)

    }

  }

}