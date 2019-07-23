package http.websockets

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.server.Directives._
import akka.stream.{ActorMaterializer, FlowShape, OverflowStrategy}
import akka.stream.scaladsl.{Flow, GraphDSL, Source}

object WebSockets2 extends App {
  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()


  val echoService : Flow[Message,Message,_] = {
//    Flow.fromGraph(GraphDSL.create(Source.actorRef[Int](bufferSize = 10, OverflowStrategy.fail)) { implicit builder =>
//      src =>
//        import GraphDSL.Implicits._
//
//        val actorSrc = builder.materializedValue.map(x => {println(s"Sending 23 to the actor $x"); x ! 23})
//
//      FlowShape()
//    })
    Flow[Message].map{
      case tx : TextMessage => println("Hello") ; TextMessage( Source.single("ECHO : ") ++ tx.textStream )
      case _ => TextMessage("Message Type Unsupported")
    }
  }

  val route = get{
    pathEndOrSingleSlash{
      complete("Welcome to Websocket Server")
    }
  } ~
  path("ws"){
    get{
      handleWebSocketMessages(echoService)
    }
  }

  Http().bindAndHandle(route,"localhost",9090)



}
