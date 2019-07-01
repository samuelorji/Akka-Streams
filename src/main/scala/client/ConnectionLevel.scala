package client

import scala.util.{Failure, Success}

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}

object ConnectionLevel extends App {

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  val connectionFlow = Http().outgoingConnection("www.google.com")

  val sink = Sink.head[HttpResponse]
  def oneOffRequest(req : HttpRequest) =
    Source.single(req).via(connectionFlow).runWith(sink)

  import system.dispatcher
  oneOffRequest(HttpRequest()) onComplete{
    case Success(res) => println(s"Result from Google is $res")
    case Failure(_)  => println(s"Failure trying to reach Google")
  }








}
