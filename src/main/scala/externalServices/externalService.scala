package externalServices

import akka.actor.{Actor, ActorSystem, Props}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import akka.util.Timeout

import com.typesafe.config.ConfigFactory

object externalService extends App {

  val config = ConfigFactory.load()
  implicit val system = ActorSystem("Externalservice",config)
  implicit val materializer = ActorMaterializer()

  implicit val dispatcher = system.dispatchers.lookup("my-dispatcher")

  case class Event(event : String, num : Int)

  val eventSource = Source(
    (1 to 10).map(x => Event(s"akka${x}", x))
  )

  println(system.dispatcher)

  class EventActor extends Actor {
    override def receive: Receive = {
      case req : Event =>
        println(s"prcocessing $req on thread ${Thread.currentThread().getName}")
        sender() ! req.toString
    }
  }

  val eventActor = system.actorOf(Props[EventActor])
  import akka.pattern.ask
  import scala.concurrent.duration._
  implicit val timeout = Timeout( 5 seconds)
 val eventFlow =  eventSource.mapAsync(4)(event => (eventActor ? event).mapTo[String])

  val eventSink = Sink.foreach[String](println)

  eventFlow.runWith(eventSink)


}
