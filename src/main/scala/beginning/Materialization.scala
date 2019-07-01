package beginning

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, OverflowStrategy}
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}

object Materialization extends App {

  implicit val system        = ActorSystem()
  implicit val materializers = ActorMaterializer()

//  val source = Source(1 to 10)
//  val flow   = Flow[Int].map(_ * 3)
//  val sink   = Sink.foreach[Int](println)
//  val sink1   = Sink.reduce[Int](_+_)
//
//  val newSource = source.via(flow).toMat(sink1)(Keep.left).run

 // val (queue,future) = Source.queue[Int](10,OverflowStrategy.fail).map(_ + 10).toMat(Sink.fold[Int,Int](0)(_ + _))(Keep.both).run()


  val getDetails : Int => String = {
    case 4 => "Hello"
  }

  println(getDetails(5))







}
