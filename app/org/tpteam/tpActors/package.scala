package org.tpteam

import akka.actor.{Cancellable, Actor}
import org.tpteam.tpActors.WSClientInnerMsgs.{Pong, Ping}
import play.api.libs.iteratee.{Concurrent, Enumerator, Iteratee}
import play.api.libs.json.{Json, JsValue}
import play.api.mvc.RequestHeader

package object tpActors {

  case class UserSessionData(
                              userSession: Any
                              )

  trait JsPushee {

    me: Actor =>

    def --->(toPush: JsValue)(implicit channel: Concurrent.Channel[JsValue]) {

      import WSClientInnerMsgs._

      try {

        channel.push(toPush)

      } catch {

        case error: Throwable =>

          play.Logger.error(s"WebSocket has encountered an error sending message $error")
          me.self ! Quit

      }
    }
  }

  object WSInnerMsgs {

    case class Connect(request: RequestHeader)

    case class Connected(iteratee: Iteratee[JsValue, _], enumerator: Enumerator[JsValue])

    case class AuthenticatedConnect(request: RequestHeader, userSession: UserSessionData)

    case object Disconnect

  }

  object WSClientMsgs {

    case class JsFromClient(elem: JsValue)(implicit _request: RequestHeader) {

      def request = _request

    }

    case class JsToClient(elem: JsValue)

  }

  object WSClientInnerMsgs {

    case class InitDone(channel: Concurrent.Channel[JsValue])

    case object Ping

    case object Pong

    case object Quit

  }

}
