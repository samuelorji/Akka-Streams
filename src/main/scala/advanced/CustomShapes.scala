package advanced

import scala.collection.immutable

import akka.actor.ActorSystem
import akka.{NotUsed, stream}
import akka.stream.scaladsl.{Balance, Broadcast, GraphDSL, Merge, RunnableGraph, Sink, Source}
import akka.stream._
import scala.concurrent.duration._

object CustomShapes extends App {

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  /**
    * Create your custom shape and define the inlets and outlets
    * @param in0
    * @param in1
    * @param out0
    * @param out1
    * @param out2
    */
  case class BalanceCust(
      in0 : Inlet[Int],
      in1 : Inlet[Int],
      out0 : Outlet[Int],
      out1 : Outlet[Int],
      out2 : Outlet[Int],
      ) extends Shape {
    override def inlets: immutable.Seq[Inlet[_]] = List(in0, in1)

    override def outlets: immutable.Seq[Outlet[_]] = List(out0,out1,out2)

    override def deepCopy(): Shape = BalanceCust(
      in0.carbonCopy(),
      in1.carbonCopy(),
      out0.carbonCopy(),
      out1.carbonCopy(),
      out2.carbonCopy()
    )
  }

  val balanceCustImpl = GraphDSL.create(){ implicit  builder =>
    import GraphDSL.Implicits._
    val merge   = builder.add(Merge[Int](2))
    val balance = builder.add(Balance[Int](3))
    merge ~> balance
    BalanceCust(
      merge.in(0),
      merge.in(1),
      balance.out(0),
      balance.out(1),
      balance.out(2)
    )

  }

  val graph =  RunnableGraph.fromGraph(
    GraphDSL.create(){ implicit builder =>

      import GraphDSL.Implicits._
      val balanceImpl = builder.add(balanceCustImpl)

      val slowSource = Source(Stream.from(1)).throttle(1, 1 second)
      val fastSource = Source(Stream.from(1)).throttle(2, 1 second)

      def createSink(index: Int) = Sink.fold(0)((count: Int, element: Int) => {
        println(s"[sink $index] Received $element, current count is $count")
        count + 1
      })

      slowSource ~> balanceImpl.in0 ; fastSource ~> balanceImpl.in1

      balanceImpl.out0 ~> createSink(1)
      balanceImpl.out1 ~> createSink(2)
      balanceImpl.out2 ~> createSink(3)

      ClosedShape
    }
  )


  case class BalanceMxN[T](inlets : List[Inlet[T]], outlets : List[Outlet[T]]) extends Shape {
//    override def inlets: immutable.Seq[Inlet[_]] = inlet
//
//    override def outlets: immutable.Seq[Outlet[_]] = outlet

    override def deepCopy(): Shape = new BalanceMxN[T](inlets.map(_.carbonCopy()), outlets.map(_.carbonCopy()))
  }

  object BalanceMxN {
    def apply[T](input : Int , output : Int) : Graph[BalanceMxN[T],NotUsed] =
      GraphDSL.create(){ implicit builder =>

        import GraphDSL.Implicits._
        val merge   = builder.add(Merge[T](input))
        val balance = builder.add(Balance[T](output))
        merge ~> balance
        BalanceMxN(merge.inlets.toList, balance.outlets.toList)
      }
  }

  val graph2 =  RunnableGraph.fromGraph(
    GraphDSL.create(){ implicit builder =>

      import GraphDSL.Implicits._
      val balanceImpl = builder.add(BalanceMxN[Int](2,3))

      val slowSource = Source(Stream.from(1)).throttle(1, 1 second)
      val fastSource = Source(Stream.from(1)).throttle(2, 1 second)

      def createSink(index: Int) = Sink.fold[Int,Int](0)((count: Int, element: Int) => {
        println(s"[sink $index] Received $element, current count is $count")
        count + 1
      })

      val sink1 = builder.add(createSink(1))
      val sink2 = builder.add(createSink(2))
      val sink3 = builder.add(createSink(3))

      slowSource ~> balanceImpl.inlets(0) ; fastSource ~> balanceImpl.inlets(1)

      balanceImpl.outlets(0) ~> sink1
      balanceImpl.outlets(1) ~> sink2
      balanceImpl.outlets(2) ~> sink3

      ClosedShape
    }
  )

  graph.run()




  //  graph.run()
//  val runnable = RunnableGraph.fromGraph(
//    GraphDSL.create(){ implicit  builder =>
//
//      import GraphDSL.Implicits._
//      val bcast = builder.add(Balance[Int](2))
//
//      val sink1 = Sink.foreach[Int](x => println(s"Sink 1 : got element : $x"))
//      val sink2 = Sink.foreach[Int](x => println(s"Sink 2 : got element : $x"))
//
//      val s1 = builder.add(sink1)
//      val s2 = builder.add(sink2)
//
//      //builder.add(sink2)
//      Source(1 to 10) ~> bcast
//       bcast ~> s1
//       bcast ~> s2
//        ClosedShape
//    }
//  )

 // runnable.run()

}
