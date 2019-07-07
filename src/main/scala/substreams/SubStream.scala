package substreams

import scala.concurrent.Future
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

      val newCount = count + 1
      println(s"Sink ${Thread.currentThread().getId} just received $word and count is $newCount ")
      newCount
  }

  //subStream.to(sink).run()

  val textSource = Source(List(
    "I love Akka Streams",
    "this is amazing",
    "learning from Rock the JVM",
    "I love Akka Streams",
    "this is amazing",
    "learning from Rock the JVM",
    "I love Akka Streams",
    "this is amazing",
    "learning from Rock the JVM",
    "I love Akka Streams",
    "this is amazing",
    "learning from Rock the JVM",
    "I love Akka Streams",
    "this is amazing",
    "learning from Rock the JVM",
    "I love Akka Streams",
    "this is amazing",
    "learning from Rock the JVM",
    "I love Akka Streams",
    "this is amazing",
    "learning from Rock the JVM",
    "I love Akka Streams",
    "this is amazing",
    "learning from Rock the JVM",
    "I love Akka Streams",
    "this is amazing",
    "learning from Rock the JVM",
    "I love Akka Streams",
    "this is amazing",
    "learning from Rock the JVM",
    "I love Akka Streams",
    "this is amazing",
    "learning from Rock the JVM",
    "I love Akka Streams",
    "this is amazing",
    "learning from Rock the JVM",
    "I love Akka Streams",
    "this is amazing",
    "learning from Rock the JVM", "I love Akka Streams",
    "this is amazing",
    "learning from Rock the JVM",
    "I love Akka Streams",
    "this is amazing",
    "learning from Rock the JVM",
    "I love Akka Streams",
    "this is amazing",
    "learning from Rock the JVM",
    "I love Akka Streams",
    "this is amazing",
    "learning from Rock the JVM",

  ))

 // val grouped

  val totalWordCount = textSource
    .groupBy(2, _.length % 2)
    .map{ word => /*println(s"Word is $word");*/ word.length}
    .mergeSubstreams
    .toMat(Sink.reduce[Int](_ + _))(Keep.right)
    .run()

  val groupedBy = textSource
    .groupBy(30,_(0))
    .to(sink)

 // val grouped = textSource.grouped(2).to(Sink.foreach[Seq[String]]{x=> println("*" * 20 ) ; println(x)})

 // grouped.run()

  textSource.conflateWithSeed[List[String]]( _ => List[String]()){
    case(list, elem) =>
      println(s"current mock buffer (List) is $list and element is $elem")
      list :+ elem
  }.map(_.mkString("[",",","]"))
    .to(Sink.foreach[String](x => {Thread.sleep(1000); println(x)}))
    .run()

  textSource.conflate[String](_ + ":" + _)
    .map(_.split(":").mkString("[",",","]")).async
    .to(Sink.foreach[String](x => {Thread.sleep(3000); println(x)}))
    //.run()



  import system.dispatcher

//
//  Source( 1 to 10).mapAsync(3)(x  => {
//    Thread.sleep(1000)
//    Future(x) andThen{
//      case Success(res) => println(s"i got result $res")
//      case Failure(ex)  => println(s"I got the exception $ex")
//    }
//  }).runWith(Sink.ignore)


//  totalWordCount onComplete{
//    case Success(res) => println(s"Total number of letters using merge Substreams is ${res}")
//    case Failure(ex)  => println(s"An exception occurred : ${ex}")
//  }

}
