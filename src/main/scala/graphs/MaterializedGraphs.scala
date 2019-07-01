package graphs

import scala.util.{Failure, Success}

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, SinkShape}
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Sink, Source}

object MaterializedGraphs extends App {

  implicit val system        = ActorSystem()
  implicit val materializers = ActorMaterializer()

  val wordSource = Source(List("The", "most", "awesome", "person", "is","samuel"))

  val lowerCaseFlow = Flow[String].filter(_.forall(_.isLower))//.fold[Int](0)((cnt,_) => cnt + 1 )

  val lessThan7Flow = Flow[String].filter(_.length < 7)

  val sinkWithCount = Sink.fold[Int,String](0)((cnt ,_) => cnt + 1)
  val printer = Sink.foreach[String](println)

  val maaterializedSink = Sink.fromGraph(
    GraphDSL.create(sinkWithCount){implicit builder => sinkWithCountShape =>

      import GraphDSL.Implicits._
      val broadcast = builder.add(Broadcast[String](2))
      broadcast ~> lowerCaseFlow ~> printer
      broadcast ~> lessThan7Flow ~> sinkWithCountShape
      SinkShape(broadcast.in)
    }
  )

  import system.dispatcher
  wordSource.runWith(maaterializedSink) onComplete{
    case Success(res) =>
      println(s"The number of items with count less than 7 is $res")
    case Failure(ex)  =>
      println("Failure getting count")
  }


}
