package beginning

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}

object Asyncboundaries extends App {

  implicit val system        = ActorSystem()
  implicit val materializers = ActorMaterializer()

  val simpleSource = Source(1 to 3)

  val simpleFlow  = Flow[Int].map{x =>println(s"Flow A got element $x on ${Thread.currentThread().getName}");Thread.sleep(2000)  ;x * 2}.async
  val simpleFlow2 = Flow[Int].map{x =>println(s"Flow B got element $x on ${Thread.currentThread().getName}");Thread.sleep(2000)  ;x * 3}.async
  val simpleFlow3 = Flow[Int].map{x =>println(s"Flow C got element $x on ${Thread.currentThread().getName}");Thread.sleep(2000)  ;x * 4}.async

  simpleSource
    .via(simpleFlow)
    .via(simpleFlow2)
    .via(simpleFlow3)
    .to(Sink.foreach(println)).run()
}
