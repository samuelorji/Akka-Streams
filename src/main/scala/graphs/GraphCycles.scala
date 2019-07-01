package graphs

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ClosedShape, SourceShape}
import akka.stream.scaladsl.{Flow, GraphDSL, Merge, MergePreferred, RunnableGraph, Source}

object GraphCycles extends  App {

  implicit val system        = ActorSystem()
  implicit val materializers = ActorMaterializer()

  val accelerator = GraphDSL.create(){ implicit builder =>

    import  GraphDSL.Implicits._
    import scala.concurrent.duration._
    val source = builder.add(Source.single(1))//.throttle(1, 2 seconds))
    val flow   = builder.add(Flow[Int].map{ x =>
      println(s"Accelerating $x")
      x + 1
    })
    val merge  = builder.add(Merge[Int](2,true))

    source ~> merge ~>            flow
              merge <~            flow

    ClosedShape
  }

  RunnableGraph.fromGraph(accelerator).run


}
