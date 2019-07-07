package advanced

import scala.collection.mutable
import scala.concurrent.{Future, Promise}
import scala.util.{Failure, Random, Success}

import akka.actor.ActorSystem
import akka.actor.SupervisorStrategy.Stop
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import akka.stream._
import akka.stream.stage._

object CustomGraphOperators extends App {

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  //the end goal is to construct a custom source that emits Random numbers
  //and a sink that prints out element in batches


   class RandomNumberSource(max : Int) extends GraphStage[SourceShape[Int]]{

    val outlet = Outlet[Int]("outlet") // create the outlet port that is used to create the shape

    val random = new Random

    override def shape: SourceShape[Int] = SourceShape(outlet)

    override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {

      setHandler(outlet,new OutHandler {
        override def onPull(): Unit =

        // indicates that the we(the source ) is free to emit a single element

        push(outlet,random.nextInt(max))


      }
      )
    }
  }


  /**
    * Please note, most Sinks would need to request upstream elements as soon as they are created:
    * this can be done by calling pull(inlet) in the preStart() callback.
    */
  //val randomSource =  Source.fromGraph(new RandomNumberSource(10)).runWith(Sink.foreach[Int](println))

  case class BatchSink[T](batchSize : Int) extends GraphStage[SinkShape[T]]{
    val inlet = Inlet[T]("inlet") //define the inlet
    override def shape: SinkShape[T] = SinkShape(inlet) //create the shape that takes the port

    override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {
      val queue = new mutable.Queue[T]

      override def preStart(): Unit = {
        println("On preStart , sending a Pull request to upstream ")
        pull(inlet) //this sends a message upStream to start sending elements downstream, at init, onPull is called on the source
      }
      setHandler(inlet, new InHandler {
        override def onPush(): Unit = {

          val element = grab(inlet)
          queue.enqueue(element)
          println("Item just pushed to the sink ")
          Thread.sleep(1000)
          if(queue.size >= batchSize){
            println(s"Queue Full : Dequeuing :::::  ${queue.dequeueAll(_ => true).mkString("[" , ",", "]")}")
          }

          //now we send a pull again because we want more elements
          pull(inlet)
        }

        override def onUpstreamFinish(): Unit = {
          if(queue.nonEmpty) println(s"Dequeuing :::::  ${queue.dequeueAll(_ => true).mkString("[" , ",", "]")}")
        }
      })
    }
  }

  val batchSink = Sink.fromGraph(BatchSink[Int](10))

 // randomSource.to(batchSink).run()



  //implement a custom flow

  case class CustFlow[T](predicate : T => Boolean) extends GraphStage[FlowShape[T,T]]{

    val inlet  = Inlet[T]("inlet")
    val outlet = Outlet[T]("outlet")

    override def shape: FlowShape[T, T] = FlowShape(inlet,outlet)

    override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {

      setHandler(outlet, new OutHandler {
        /**
          * This is where it begins (the stream is initiated first from downstream to upstream)
          * the sink that this flow is connected to initially tries to pull on its input port which is the output port of this flow
          * it's this method that then just tries to fetch data from its input port*/
        override def onPull(): Unit = {
          pull(inlet)
          /**We pull on the inlet, more or less asking the source for another element,
             and nothing passes until something is available at the input port (OnPush has been called on the inhandler by the source)
             having emitted an element*/
        }
      })
      setHandler(inlet,new InHandler {
        override def onPush(): Unit = {
          /**
           Source calls this callback to signal that there is an element available ,
          it means there is an element available on the input port and should thus be used
            */
          val element = grab(inlet) /** We Then grab the element */
          /**
            * If predicate is successful ,the outlet's handler pulls on the inlet for us
            * But if the predicate was not successful, the inlet handler does the pulling for us*/

            if (predicate(element)) {
              /** if it satisfies our predicate, we push it to the outlet, which pushes to the sink
                * and after the sink has consumed, it pulls on the output port of this flow which pulls on the inlet for us */
              //push(outlet, element)
              failStage (new RuntimeException("x cannot be 10 "))
            } else {
              /** So Since the nothing was pushed to the outlet, we pull on the inlet for another element */

             // pull(inlet)
              push(outlet,element)
          }
        }
      })


    }
  }


  val flowStage = Flow.fromGraph(CustFlow[Int](_ == 10)
    .withAttributes(ActorAttributes.supervisionStrategy{ case ex: RuntimeException => {println(s"Got an exception : ${ex.getMessage}");Supervision.Resume}}))
  //    .recover{
//    case x : RuntimeException => println(s"Got exception ${x}") ; Supervision.Resume
//  }


    //val flowWithSupervisor = flowStage.

 // Source(2 to 20).via(flowStage).runWith(Sink.foreach(println))

  class CustomFlowMaterialized[T] extends GraphStageWithMaterializedValue[FlowShape[T,T],Future[Int]] {

    val inlet  = Inlet[T]("inlet")
    val outlet = Outlet[T]("outlet")

    override val shape: FlowShape[T, T] = FlowShape[T,T](inlet,outlet)

    override def createLogicAndMaterializedValue(inheritedAttributes: Attributes): (GraphStageLogic, Future[Int]) = {

      val promise = Promise[Int]

      val graphStageLogic = new GraphStageLogic(shape) {

        private var counter = 0

        setHandler(outlet, new OutHandler {
          override def onPull(): Unit =
            pull(inlet)

          override def onDownstreamFinish(): Unit = {
            promise.success(counter)
            super.onDownstreamFinish()
          }
        })

        setHandler(inlet, new InHandler {
          override def onPush(): Unit = {
            val element = grab(inlet)
            counter += 1
            push(outlet,element)
          }

          override def onUpstreamFailure(ex: Throwable): Unit = {
            promise.failure(ex)
            super.onUpstreamFailure(ex)
          }
          override def onUpstreamFinish(): Unit = {
            promise.success(counter)
            super.onUpstreamFinish()
          }
        })
      }
      (graphStageLogic,promise.future)
    }
  }

  val customFlowMaterialized = Flow.fromGraph(new CustomFlowMaterialized[Int])

 val countFut =  Source( 1 to 20)
   .map(x => if(x == 17) throw new RuntimeException("gotcha from source") else x)
    .viaMat(customFlowMaterialized)(Keep.right)
    .to(Sink.foreach[Int](println))
    .run()



  import system.dispatcher
  countFut onComplete{
    case Success(value) =>  println(s"The number of elements that passed through $value")
    case Failure(ex)    => println(s"Encountered an error ${ex.getMessage}")
  }


}
