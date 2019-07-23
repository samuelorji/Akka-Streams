package playground

import akka.actor.{Actor, ActorSystem, Props}
import akka.stream.{ActorMaterializer, FlowShape, OverflowStrategy}
import akka.stream.scaladsl.{Flow, GraphDSL, Sink, Source}

object testing extends App {
  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  case object GoodBye
  class SimpleActor extends Actor {
    override def receive: Receive = {
      case msg : Int =>
        println(s"Actor just received Int value $msg")
        sender() ! msg

      case GoodBye =>
        println("received message Goodbye")
    }
  }

  val simpleActor = system.actorOf(Props[SimpleActor])



 val fGraph =  Flow.fromGraph(GraphDSL.create(Source.actorRef[Int](bufferSize = 10,OverflowStrategy.fail)){implicit builder => src =>

    import GraphDSL.Implicits._

   val ignoreSink = builder.add(Flow[Int].map(x => {println(s"received data $x from the actor created by this flow ") ;4 }))

    val actorSrc = builder.materializedValue.map(x => {println(s"changing from actor Ref ${x} to 56"); 56})

    val sink = builder.add(Sink.foreach(println))

   val simpleActorSink = Sink.actorRef(simpleActor,GoodBye)

    actorSrc ~> sink


   val flow = builder.add(Flow[Int].map(x => {println(s"Received number $x") ; x}))

   flow ~> simpleActorSink
   src ~> ignoreSink

    FlowShape(flow.in,ignoreSink.out)
  })

  for (x <- 1 to 2)Source.single(x).via(fGraph).to(Sink.foreach(println)).run()

}
