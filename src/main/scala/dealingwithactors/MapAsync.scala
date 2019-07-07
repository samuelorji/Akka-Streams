package dealingwithactors

import java.util.Date

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import akka.util.Timeout

object MapAsync extends App {

  implicit val system        = ActorSystem("IntWithActors")
  implicit val materializers = ActorMaterializer()


  case class Event(ev : String , date : Date = new Date)

  class PagerActor extends Actor with ActorLogging {


    private val engineers = Array("samuel","shola","dayo")

    private val engineerEmails = Map (
      "samuel" -> "samuel@yahoo.com",
      "shola"  -> "shola@yahoo.com",
      "dayo"   -> "dayo@yahoo.com"
    )

    private def processEvent(event : Event) : String = {

      val engineerIndex = (event.date.toInstant.getEpochSecond / (24 * 3600)) % engineers.length
      val engineerEmail = engineerEmails(engineers(engineerIndex.toInt))

      println(s"Sending Engineer $engineerEmail the event $event")
      Thread.sleep(1000)
      engineerEmail
    }

    override def receive: Receive = {
      case event : Event =>
        sender() ! processEvent(event)
    }
  }


  val eventList = List(
    Event("Redis Down"),
    Event("MySql Down"),
    Event("PostGres Down"),
    Event("Cassandra Down"),
    Event("RabbitMq Down")
  )

  val pagerActor = system.actorOf(Props[PagerActor])


  import akka.pattern.ask
  import scala.concurrent.duration._
  implicit val timeout = Timeout(5 seconds)
   //you can use either mapAsync or mapAsyncUnordered , with unordered, the results come in without ordering .
  // But with async....the results are first collected then ordered
  Source(eventList).mapAsyncUnordered(4)(event => (pagerActor ? event).mapTo[String]).runWith(Sink.foreach[String](x => println(s"Successfully sent email to $x")) )
}
