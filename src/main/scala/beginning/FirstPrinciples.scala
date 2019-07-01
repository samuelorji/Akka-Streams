package beginning

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}

object FirstPrinciples extends App{

  implicit val system = ActorSystem()
  implicit val materializers = ActorMaterializer()

  val source = Source[Int](1 to 20)
  val sink   = Sink.foreach[Int](println)

  val graph = source.to(sink)

   graph.run()

  val namesSource = Source(List("Anna","meriggl","junet", "minety", "chibett","Michael","Innocent","Jen"))

  val sink1 = Sink.foreach(println)

  namesSource.filter(_.length < 5).take(2).to(sink1).run()
}
