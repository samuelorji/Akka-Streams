package graphs

import java.util.Date

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ClosedShape, UniformFanInShape}
import akka.stream.scaladsl.{GraphDSL, RunnableGraph, Sink, Source, ZipWith}

object GraphShapes extends App {

  implicit val system        = ActorSystem()
  implicit val materializers = ActorMaterializer()

  /*
  goal is to find the max amongst a stream of Ints
   */

  val maxStaticGraph = GraphDSL.create(){ implicit builder =>
    import GraphDSL.Implicits._
    val zip1 = builder.add(ZipWith[Int,Int,Int](Math.max))
    val zip2 = builder.add(ZipWith[Int,Int,Int](Math.max))
6
    zip1.out ~> zip2.in0

    UniformFanInShape(zip2.out,zip1.in0,zip2.in1,zip1.in1)

  }

  val date = new Date

  val source1 = Source(1 to 10)
  val source2 = Source(1 to 10 map (_ => 5))
  val source3 = Source(1 to 10 reverse)

  val sink = Sink.foreach(println)
  val runnableGraph = RunnableGraph.fromGraph(
    GraphDSL.create(){ implicit builder =>
      import GraphDSL.Implicits._

      val maxGraph = builder.add(maxStaticGraph)
      source1 ~> maxGraph.in(0)
      source2 ~> maxGraph.in(1)
      source3 ~> maxGraph.in(2)

      maxGraph.out ~> sink


      ClosedShape
    })

  runnableGraph.run()

}
