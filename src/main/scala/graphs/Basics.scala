package graphs

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ClosedShape}
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Merge, RunnableGraph, Sink, Source, Zip}

object Basics extends App {
  implicit val system        = ActorSystem()
  implicit val materializers = ActorMaterializer()
  val input                  = Source(1 to 1000)
  val incrementer            = Flow[Int].map(_ + 1)
  val multiplier             = Flow[Int].map(_ * 10)
  val sink                   = Sink.foreach[(Int,Int)](println)

  val sink1 = Sink.foreach[Int]{x => println(s"Sink 1 : $x")}
  val sink2 = Sink.foreach[Int]{x => println(s"Sink 2 : $x")}

  val graphWithZip = RunnableGraph.fromGraph(
    GraphDSL.create(){ implicit builder: GraphDSL.Builder[NotUsed] =>
      import GraphDSL.Implicits._

      //builder is mutable ...and we mutate it until


      val broadcast = builder.add(Broadcast[Int](2))
     // val merge       = builder.add(Merge[Int](2))

//      input ~> broadcast
//
//      broadcast.out(0) ~> incrementer ~> zip.in0
//      broadcast.out(1) ~> multiplier  ~> zip.in1
//
//      zip.out ~> sink

      val zip = builder.add(Zip[Int,Int])

      input ~> broadcast
      broadcast ~> incrementer ~> zip.in0
      broadcast ~> multiplier  ~> zip.in1
      zip.out ~> sink
      ClosedShape
    }
  )

 // graphWithZip.run()

  import scala.concurrent.duration._
  val fastSource = Source(1 to 20)
  val slowSource = Source(21 to 50)

  val graphWithMerge = RunnableGraph.fromGraph(
    GraphDSL.create(){implicit builder =>
      import GraphDSL.Implicits._

     // val broadcast = builder.add(Broadcast[Int](2))
      val merge     = builder.add(Merge[Int](2))

      fastSource ~> merge ~> sink1
      slowSource ~> merge
      ClosedShape
    }
  )

  graphWithMerge.run

}
