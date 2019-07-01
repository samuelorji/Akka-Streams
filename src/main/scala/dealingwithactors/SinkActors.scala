package dealingwithactors

import akka.actor.{Actor, ActorLogging, ActorSystem, PoisonPill, Props}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}

object SinkActors extends App {
  implicit val system        = ActorSystem()
  implicit val materializers = ActorMaterializer()

  object SinkActor {
    case object StreamInit
    case object StreamAck
    case object StreamComplete
    case class StreamFailed(ex : Throwable)
  }
  class SinkActor extends Actor with ActorLogging {

    import SinkActor._
    override def receive: Receive = {
      case StreamInit =>
        log.info("Stream Initialized") // this is initially sent to the stream to indicate a flow of data
        sender() ! StreamAck // this is needed so data can now flow

      case StreamComplete =>
        log.info("Stream has been completed")
        self ! PoisonPill

      case req : StreamFailed =>
        log.error(s"received an error in the stream ${req.ex.getLocalizedMessage}")
        self ! PoisonPill


      case msg =>
        log.info(s"Just got the message $msg")
        sender()  ! StreamAck // this has to be sent back into the stream, if not, it'll backpressure after receiving the first element
    }
  }


  val sinkActor = system.actorOf(Props[SinkActor])

  import SinkActor._
 val sinkActorAck =  Sink.actorRefWithAck(
    ref = sinkActor,
    onInitMessage = StreamInit,
    ackMessage    = StreamAck,
    onCompleteMessage = StreamComplete,
    onFailureMessage = ex => StreamFailed(ex)
  )

  Source(List(1,2)).runWith(sinkActorAck)

}
