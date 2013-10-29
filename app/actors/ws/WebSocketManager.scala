package actors.ws

import akka.actor._
import play.api.libs.json._
import play.api.libs.iteratee._
import play.api.libs.concurrent._
import akka.util.Timeout
import akka.pattern.ask
import play.api.Play.current
import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits._
import scala.concurrent.duration._

abstract class WebSocketManager[M <: WSManagerActor](implicit ct: scala.reflect.ClassTag[M]) {
  
  val name: Option[String] = None
 
  private val _actor =
    (name) match {
    	case Some(n) => Akka.system.actorOf(Props[M],n)
    	case _ => Akka.system.actorOf(Props[M])
  	}
  
  def actor: ActorRef = _actor
  
  implicit val timeout = Timeout(30 seconds)
  
  import WSInnerMsgs._
  def control(implicit request: RequestHeader):scala.concurrent.Future[(Iteratee[JsValue,_],Enumerator[JsValue])] = {
	  	(actor ? Connect(request)).map {
		  	case Connected(iteratee,enumerator) => 
		  	  	(iteratee,enumerator)   
    }
  }
  
}

object WSInnerMsgs {
  case class Connect(request: RequestHeader)
  case class Connected(iteratee: Iteratee[JsValue,_],enumerator: Enumerator[JsValue])
  case object Disconnect
}

abstract class WSManagerActor extends Actor {
  
  import WSInnerMsgs._
  
  val initTimeout = 2 seconds
  val browserTimeout = 500 milliseconds 
  
  def receive = {
    connectionManagement orElse
    dispatch  
  }
  
  def dispatch: PartialFunction[Any,Unit] = {
    case msg =>
      context.children.foreach(act => act ! msg)
  }
  
  def operative(implicit request: RequestHeader) :
	  ((ActorRef) => PartialFunction[Any,Unit])
  
  def connectionManagement: PartialFunction[Any,Unit] = {
    case Connect(r) => 
      	import WSClientInnerMsgs._
      	implicit val request: RequestHeader = r
      	val act: ActorRef = context.actorOf(Props(
      								new WsClientActor(
      										initTimeout,
      										browserTimeout,
      										operative)(r)
      							))
      	val outChannel: Enumerator[JsValue] = 
      	Concurrent.unicast[JsValue](
      			onStart = (
      					(c) => {
      						act ! InitDone(c)
      					}),
      			onComplete = () => act ! Quit,
      			onError = {(_,_) => act ! Quit})
      	val inChannel:  Iteratee[JsValue,Unit] = 
      		Iteratee.foreach[JsValue](msg =>
      		  (msg.\("pong").asOpt[Boolean]) match {
      		    case Some(true) => act ! Pong
      		    case _ =>
      		      import WSClientMsgs._
      		      act ! JsFromClient(msg)
      		  })
     
      	sender ! Connected(inChannel,outChannel)
    case Disconnect =>
      context.stop(sender)  
  }
  
}
object WSClientMsgs {
  	case class JsFromClient(elem: JsValue)
  	case class JsToClient(elem: JsValue)
}
object WSClientInnerMsgs {
  	case class InitDone(channel: Concurrent.Channel[JsValue])
  	case object Ping
  	case object Pong
  	case object Quit
}

trait JsPushee {
  me : Actor =>
  def --->(topush: JsValue)(implicit channel: Concurrent.Channel[JsValue]) {
	  import WSClientInnerMsgs._
      try channel.push(topush) catch {case _ : Throwable => me.self ! Quit}
  }
}

class WsClientActor(
	  initTimeout: FiniteDuration,
	  browserTimeout: FiniteDuration,
      operative: ((ActorRef) => PartialFunction[Any,Unit]))
      (implicit request: RequestHeader) extends Actor with JsPushee {
	
	import WSClientInnerMsgs._
    	
    def receive = {
	  case InitDone(channel) =>
	    rescheduleOperative(initTimeout)(channel)
	  case any => 
	    context.system.scheduler.scheduleOnce(50 milliseconds)(
	        self forward any)
  	}
	
	def rescheduleOperative(timeout: FiniteDuration)(implicit channel: Concurrent.Channel[JsValue]): Unit = { 
	  val newNextStop = 
  	      	context.system.scheduler.scheduleOnce(browserTimeout, self, Quit)
  	    context.become(operativePingPong(newNextStop), true)
  	    self ! Ping
	}
	def rescheduleOperative(implicit channel: Concurrent.Channel[JsValue]): Unit = 
	  rescheduleOperative(browserTimeout)
  	
  	
  	def operativePingPong(nextStop: Cancellable)(implicit channel: Concurrent.Channel[JsValue]): PartialFunction[Any,Unit] = {
  	  pingReceive orElse 
  	  pongReceive(nextStop) orElse 
  	  quitReceive orElse 
  	  jsToClientReceive orElse
  	  operative.apply(self)
  	}
	
	import WSClientMsgs._
	def jsToClientReceive(implicit channel: Concurrent.Channel[JsValue]): PartialFunction[Any,Unit] = {
  	  case JsToClient(js) =>
  	    --->(js)
  	}
  	
  	def pingReceive(implicit channel: Concurrent.Channel[JsValue]): PartialFunction[Any,Unit] = {
  	  case Ping =>
  	    --->(Json.obj("ping" -> true))
  	}
  	
  	def pongReceive(nextStop: Cancellable)(implicit channel: Concurrent.Channel[JsValue]): PartialFunction[Any,Unit] = {
  	  case Pong =>
  	    nextStop.cancel
  	    rescheduleOperative
  	}
  	
  	def quitReceive(implicit channel: Concurrent.Channel[JsValue]): PartialFunction[Any,Unit] = {
  	  case Quit =>
  	     channel.eofAndEnd
  	     context.stop(self)
  	}

  }