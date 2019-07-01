package faultTolerance

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}

object faults extends App {

  implicit val system  = ActorSystem("Faults")
  implicit val materializers = ActorMaterializer()


  val faultySource  = Source(1 to 10).map(x => if(x == 6) throw new RuntimeException else x)
  faultySource.log("faultySource")
    //.runWith(Sink.ignore)

  //system.terminate()

  faultySource.recover{ //using recover uses the partial fraction to deal with an error and return a value which then terminates the stream
    case _ : RuntimeException => 90
  } .log("Recover")
    .runWith(Sink.ignore)

  faultySource.recoverWithRetries( //after n number of tries, it returns a new source based on the exception thrown
    3, {case _ : RuntimeException => Source( 11 to 15)}
  ).log("Recover").runWith(Sink.ignore)
}
