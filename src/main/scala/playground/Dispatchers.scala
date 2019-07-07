package playground

import java.util.Random

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.stream.scaladsl.{Flow, GraphDSL, Partition, RunnableGraph, Source}
import akka.stream.{ActorMaterializer, ClosedShape}

import com.typesafe.config.ConfigFactory


object Dispatchers extends App {
  val config = ConfigFactory.load()
  implicit val system        = ActorSystem("Dispatchers",config)
  implicit val materializers = ActorMaterializer()

  class SimpleActor extends Actor with ActorLogging {
    var count  = 0
    def receive = {
      case msg =>
        count += 1
        log.info(s"[$count] : [$msg]")
    }
  }

  val actors = (1 to 10).map(x => system.actorOf(Props[SimpleActor].withDispatcher("my-dispatcher"), s"actor_$x"))

  val random = new Random()
  for(i <- 1 to 1000){
    actors(random.nextInt(10)) ! i
  }






}
