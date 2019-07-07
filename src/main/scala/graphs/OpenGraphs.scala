package graphs

import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl.{Broadcast, Concat, Flow, GraphDSL, Partition, RunnableGraph, Sink, Source}

object OpenGraphs extends  App {
  implicit val system        = ActorSystem()
  implicit val materializers = ActorMaterializer()

  /*
  Make concatenated Source
   */
  val source1      = Source(1 to 20)
  val source2      = Source(21 to 40)
  val source3      = Source(41 to 60)

  val concatSource = Source.fromGraph(
    GraphDSL.create(){ implicit builder =>
      import GraphDSL.Implicits._

      val concat = builder.add(Concat[Int](3))

      source1 ~> concat
      source2 ~> concat
      source3 ~> concat
      SourceShape(concat.out)
  })

  val simpleSink = Sink.foreach(println)

 // concatSource.runWith(simpleSink)

//  val sink1 = Sink.foreach[Int](x =>  println(s"Sink 1 : $x"))
//  val sink2 = Sink.foreach[Int](x =>  println(s"Sink 2 : $x"))
//  val broadcastSink  = Sink.fromGraph(
//    GraphDSL.create(){ implicit builder =>
//      import GraphDSL.Implicits._
//      val broadcast = builder.add(Broadcast[Int](2))
//
//      broadcast ~> sink1
//      broadcast ~> sink2
//
//      SinkShape(broadcast.in)
//    }
//  )

  val incrementer            = Flow[Int].map(_ + 1)
  val multiplier             = Flow[Int].map(_ * 10)

  //source1.runWith(broadcastSink)


  val flowGraph = Flow.fromGraph(
    GraphDSL.create(){ implicit builder =>
      import GraphDSL.Implicits._

      val incrementerShape = builder.add(incrementer)
      val multiplierShape  = builder.add(multiplier)
      incrementerShape ~> multiplierShape

      FlowShape(incrementerShape.in,multiplierShape.out)
  })

  case class Transaction(id : String , amnt : Int)

  val transactionSource = ( 1 to 10).map(x => Transaction(x.toString, x * 10000))

  val suspiciousFlow     = Flow[Transaction].filter(_.amnt < 50000).map(_.id)
  val nonSuspiciousFlow  = Flow[Transaction].filter(_.amnt >= 50000).map(_.id)

  val sink1  = Sink.foreach[String](x => println(s"Sink 1 : Suspicious    : $x"))
  val sink2  = Sink.foreach[String](x => println(s"Sink 2 : NonSuspicious : $x"))
  val transSink    = Sink.foreach[Transaction](x => println(s"Sink 3 : All Transactions $x"))


  val checker = GraphDSL.create(){implicit builder =>
    val partitioner = builder.add(Partition[Transaction](2, transaction => if (transaction.amnt > 50000) 0 else 1))

    new FanOutShape2[Transaction,Transaction,Transaction](partitioner.in,partitioner.out(0),partitioner.out(1))
  }

  val graph = RunnableGraph.fromGraph(
    GraphDSL.create(){implicit builder =>
    import GraphDSL.Implicits._
    val checkerShape = builder.add(checker)
    val suspiciousIdMapper = builder.add(Flow[Transaction].map(_.id))
    val NonSuspiciousIdMapper = builder.add(Flow[Transaction].map(_.id))
    Source(transactionSource) ~> checkerShape.in

    checkerShape.out0  ~>  suspiciousIdMapper    ~> sink1
    checkerShape.out1  ~>  NonSuspiciousIdMapper ~> sink2
    ClosedShape


  }
  )
  graph.run()

  val checkFlow = GraphDSL.create(){ implicit builder =>

    import GraphDSL.Implicits._
    val broadcast = builder.add(Broadcast[Transaction](3))

    val suspiciousFlowShape    = builder.add(suspiciousFlow)
    val nonSuspiciousFlowShape = builder.add(nonSuspiciousFlow)
    broadcast ~> suspiciousFlowShape
    broadcast ~> nonSuspiciousFlowShape
    new FanOutShape3(broadcast.in,suspiciousFlowShape.out,nonSuspiciousFlowShape.out,broadcast.out(2))
  }

  val runGraph = RunnableGraph.fromGraph(
    GraphDSL.create(){ implicit builder =>
      import GraphDSL.Implicits._

      val checkFlowShape = builder.add(checkFlow)

      Source(transactionSource) ~> checkFlowShape.in
      checkFlowShape.out0 ~> sink1
      checkFlowShape.out1 ~> sink2
      checkFlowShape.out2 ~> transSink
      ClosedShape
    }
  )

  runGraph.run()




}
