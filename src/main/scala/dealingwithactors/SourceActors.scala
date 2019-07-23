package dealingwithactors

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, OverflowStrategy}
import akka.stream.scaladsl.{Keep, Sink, Source}

object SourceActors extends App {

  implicit val system        = ActorSystem()
  implicit val materializers = ActorMaterializer()

  val sourceActorConfig = Source.actorRef[Int](16,OverflowStrategy.dropNew) //create your actor with the right config
  val sourceActorGraph  = sourceActorConfig.toMat(Sink.foreach[Int](num => println(s"Got the number $num")))(Keep.left)


  val sourceActor = sourceActorGraph.run()

  (1 to 20).foreach(x =>  sourceActor ! x )





}
