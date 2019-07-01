package beginning

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, OverflowStrategy}
import akka.stream.scaladsl.{Flow, Sink, Source}

object Backpressure extends App  {

  implicit val system        = ActorSystem()
  implicit val materializers = ActorMaterializer()

  val fastSource = Source(1 to 100)
  val slowSink   = Sink.foreach[Int] { x =>
    Thread.sleep(1000)
    println(s"Sink : $x")
  }

  val simpleFlow  = Flow[Int].map{x =>
    println(s"Incoming : $x")
    x + 1
  }

//  fastSource.via(simpleFlow).async
//    .to(slowSink).run()

  val bufferedFlow = simpleFlow.buffer(10,OverflowStrategy.dropNew)

  fastSource.async.via(bufferedFlow).async.to(slowSink).run()

  //system.scheduler.


}
