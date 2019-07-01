package test

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import akka.testkit.{TestKit, TestProbe}

import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, Matchers, WordSpecLike}
import akka.pattern._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

import akka.stream.testkit.scaladsl.{TestSink, TestSource}

class StreamTest extends TestKit(ActorSystem("Streams"))
  with WordSpecLike
  with BeforeAndAfterAll
  with Matchers {

 implicit val materializer = ActorMaterializer()

  override def afterAll(): Unit = TestKit.shutdownActorSystem(system)


  "A Simple Stream " must {
    "integrate with a test actor based sink " in {
      val simpleSource = Source(1 to 10)
      val flow         = Flow[Int].scan(0)(_ + _) //0 , 1 , 3, 6, 10 , 15
      val stream       = simpleSource.via(flow)
      val probe        = TestProbe()
      val probeSink    = Sink.actorRef(probe.ref , "Completion Message")

      stream.to(probeSink).run()
      probe.expectMsgAllOf(0,1,3,6,10,15)

    }

    "send materialized values to an actor " in {
      val source  = Source(1 to 10)
      val matSink = Sink.fold[Int,Int](0)(_ + _)

      val probe = TestProbe()
      source.toMat(matSink)(Keep.right).run().pipeTo(probe.ref)
      probe.expectMsg(55)
    }

    "integrate with Streams TestKt Sink " in {
      val source = Source(1 to 5).map(_ * 2)
      val testSink = TestSink.probe[Int]

      val graph = source.runWith(testSink)
      graph.request(6) // get 6 elements from the graph
        .expectNext(2,4,6,8,10) // these are the elements we expect from the stream
        .expectComplete() // more like expectNoMsg
    }

    "integrate with Streams Test-Kit Source" in {
      val sinkWithException = Sink.foreach[Int]{
        case 12 => throw new RuntimeException("Not allowed")
        case _ =>
      }

      val testSource = TestSource.probe[Int]

      val materializedGraph = testSource.toMat(sinkWithException)(Keep.both).run()

      val (testSourcePublisher, resultFut) = materializedGraph

      testSourcePublisher
        .sendNext(1)
        .sendNext(2)
        .sendNext(5)
        .sendNext(12)
        .sendNext(123)
        .sendComplete()

      resultFut onComplete{
        case Success(_) => fail() //this is used to signify that this test should have failed
        case Failure(_) =>
      }
    }

    "integrate with stream and source" in {
      val flow = Flow[Int].map(_ * 2)
      val testSource = TestSource.probe[Int]
      val testSink    = TestSink.probe[Int]

      val graph = testSource.via(flow).toMat(testSink)(Keep.both).run()

      val (publisher , subscriber) = graph

      publisher
        .sendNext(1)
        .sendNext(2)
        .sendNext(3)
        .sendNext(4)
        .sendNext(5)
        .sendComplete()

      subscriber
        .request(5)
        .expectNext(2)
        .expectNext(4)
        .expectNext(6)
        .expectNext(8)
        .expectNext(10)
        .expectComplete()


    }
  }

}
