package graphs

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, BidiShape, ClosedShape}
import akka.stream.scaladsl.{Flow, GraphDSL, RunnableGraph, Sink, Source}

object BidirectionalFlow extends App {

  implicit val system        = ActorSystem()
  implicit val materializers = ActorMaterializer()

  def encrypt(n : Int)(word : String) : String = word.map(c => (c + n).toChar).toString
  def decrypt(n : Int)(word : String) : String = word.map(c => (c - n).toChar).toString

  val encryptFlow = Flow[String].map(encrypt(3))
  val decryptFlow = Flow[String].map(decrypt(3))

  val dencryptedource = Source(List("akka","is","awesome","testing","bidirectional","flows"))
  val encryptedource   = Source(List("akka","is","awesome","testing","bidirectional","flows").map(encrypt(3)))
  val bidirectionalFlowGraph = GraphDSL.create(){ implicit builder =>
    import GraphDSL.Implicits._
    val encryptFlowShape = builder.add(encryptFlow)
    val decryptFlowShape = builder.add(decryptFlow)
    BidiShape(encryptFlowShape.in,encryptFlowShape.out,decryptFlowShape.in,decryptFlowShape.out)
  }

  val encryptionSink = Sink.foreach[String](x =>  println(s"Encryption Sink : $x "))
  val decryptionSink = Sink.foreach[String](x =>  println(s"Decryption Sink : $x "))
  val runnableG = RunnableGraph.fromGraph(
    GraphDSL.create(){ implicit builder =>

      import GraphDSL.Implicits._
      val bidiShape = builder.add(bidirectionalFlowGraph)
      val encryptionSourceShape = builder.add(encryptedource)
      val decryptionSourceShape = builder.add(dencryptedource)
      decryptionSourceShape ~> bidiShape.in1           ;  bidiShape.out1 ~> encryptionSink
      decryptionSink        <~ bidiShape.out2          ;  bidiShape.in2 <~  encryptionSourceShape

      ClosedShape
    }
  )

  runnableG.run()

}
