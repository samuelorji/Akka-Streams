package substreams

import scala.util.{Failure, Success}

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}

object SubStream extends App {
  implicit val system = ActorSystem("SubStreams")
  implicit val materializer = ActorMaterializer()

  val wordSource = Source(List("akka", "is","awesome","you", "guys","are","here"))

  val subStream  = wordSource.groupBy(10,word => if(word.isEmpty) '\0' else word(0))

  val sink = Sink.fold[Int,String](0){
    case (count, word) =>
      println(s"This Sink is on ${Thread.currentThread().getName}")
      val newCount = count + 1
      println(s"I just received $word and count is $newCount ")
      newCount
  }

  //subStream.to(sink).run()

  val textSource = Source(List(
    "I love Akka Streams",
    "this is amazing",
    "learning from Rock the JVM"
  ))

  val totalWordCount = textSource
    .groupBy(2, _.length % 2)
    .map{ word => /*println(s"Word is $word");*/ word.length}
    .mergeSubstreams
    .toMat(Sink.reduce[Int](_ + _))(Keep.right)
    .run()

  val flatMapWordCount = textSource
    .flatMapConcat{word =>  println(s"Word is $word and word length is ${word.length}") ; Source(0 to word.length)}.via(Flow[Int].reduce[Int](_ + _) )
    .toMat(Sink.fold[Int,Int](0){
      case (acc, wordC) =>
        println(s"Accumulator is $acc and wordCount is $wordC")
         acc +  wordC
    })(Keep.right)
    .run()



  import system.dispatcher
  flatMapWordCount onComplete{
    case Success(res) => println(s"Total number of letters using FlatMap is ${res}")
    case Failure(ex)  => println(s"An exception occurred : ${ex}")
  }

  totalWordCount onComplete{
    case Success(res) => println(s"Total number of letters using merge Substreams is ${res}")
    case Failure(ex)  => println(s"An exception occurred : ${ex}")
  }

}
