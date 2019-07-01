package dealingwithactors

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.util.Timeout

object FlowActors extends App {

  implicit val system        = ActorSystem("IntWithActors")
  implicit val materializers = ActorMaterializer()


  val numberSource = Source(1 to 10)

  class SimpleActor extends Actor with ActorLogging {

    override def receive: Receive = {
      case s : Int =>
        val currentSender = sender()
        log.info(s"Just received a number $s")
        currentSender ! (2 * s)
      case _ =>
    }
  }

  val simpleactor = system.actorOf(Props[SimpleActor], "SimpleActor")

  import scala.concurrent.duration._
  implicit val timeout = Timeout(5 seconds)
  val actorFlow = Flow[Int].ask[Int](4)(simpleactor)

 // numberSource.via(actorFlow).to(Sink.ignore).run
  numberSource.ask[Int](4)(simpleactor).runWith(Sink.ignore)
}
