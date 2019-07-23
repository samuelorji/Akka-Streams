package http.websockets

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.server.Directives._
import akka.stream.{ActorMaterializer, FlowShape, OverflowStrategy}
import akka.stream.scaladsl.{Flow, GraphDSL, Merge, Sink, Source}

import http.websockets.WebSockets2.echoService

object Advanced extends App {
  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  sealed trait ChatEvent

  implicit def chatEventToChatMessage(event: IncomingMessage): ChatMessage = ChatMessage(event.user, event.iText)

  case class IncomingMessage(user : String, iText : String) extends ChatEvent

  case class UserJoined(name : String , ref : ActorRef)    extends ChatEvent

  case class ChatMessage(sender: String, text: String)

  object SystemMessage {
    def apply(text: String) = ChatMessage("System", text)
  }
  case class UserLeft(name : String)

  class ChatRoom(roomId : Int, actorSystem : ActorSystem){
    private[this] val chatRoomActor = actorSystem.actorOf(Props(new ChatRoomActor(roomId)))

    def websocketFlow(user : String) : Flow[Message,Message,_] = {
      Flow.fromGraph(GraphDSL.create(Source.actorRef[ChatMessage](bufferSize = 10,OverflowStrategy.fail)){implicit  builder => src =>

        /**
          * Once the stream starts , it creates an actor for the stream,
          */
        import GraphDSL.Implicits._

       // val chatSource = builder.add(Source.actorRef[ChatMessage](bufferSize = 10,OverflowStrategy.fail))
        val fromWebSocket = builder.add(
          Flow[Message].collect{
            case TextMessage.Strict(txt) => IncomingMessage(user,txt)
          }
        )

        val backToWebSocket = builder.add(
          Flow[ChatMessage].map{
            case ChatMessage(auth, text) => TextMessage(s"[$auth] : $text")
          }
        )

        val chatActorSink = Sink.actorRef[ChatEvent](chatRoomActor,UserLeft(user)) // what goes is Either incoming message from websocket or User Joined from materialized actor

        val merge = builder.add(Merge[ChatEvent](2))

        val actorAsSource = builder.materializedValue.map(actor => UserJoined(user,actor)) // this is materialized once for each incoming connection

        fromWebSocket ~> merge //merge from web socket

        actorAsSource ~> merge //for each materialized value ...send a User Joined to the Sink

        merge ~> chatActorSink //The chat room actor for this class gets all the messages coming out from this merger (all msgs are of type Message)

        src ~> backToWebSocket //now whatever message that comes in through the source we send back to the websocket

        FlowShape(fromWebSocket.in,backToWebSocket.out)
      })
    }

    def sendMessage(message : ChatMessage) : Unit = chatRoomActor ! message
  }
  object ChatRoom {
    def apply(roomId: Int)(implicit system: ActorSystem) = new ChatRoom(roomId, system)
  }

  object ChatRooms {
    var chatRooms : Map[Int,ChatRoom] = Map.empty[Int,ChatRoom]

    def findOrCreate(number : Int)(implicit system : ActorSystem) : ChatRoom = chatRooms.getOrElse(number,createNewChatRoom(number))

    private def createNewChatRoom(i: Int)(implicit system: ActorSystem) : ChatRoom = {
      val chatRoom = ChatRoom(i)
      chatRooms += i -> chatRoom
      chatRoom
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
    } ~
  path("wschat" / IntNumber){ chatId =>
    parameter('name){username =>
     handleWebSocketMessages(ChatRooms.findOrCreate(chatId).websocketFlow(username))
    }
  }

  class ChatRoomActor(roomId : Int) extends Actor {
    var participants : Map[String, ActorRef] = Map.empty[String,ActorRef]

    override def receive: Receive = {
      case UserJoined(name , actorRef) =>
        participants += name -> actorRef
        broadcast(SystemMessage(s"User $name joined channel ..... "))
        println(s"User $name joined channel[$roomId]")

      case UserLeft(name) =>
        println(s"User $name left channel[$roomId]")
        broadcast(SystemMessage(s"User $name left channel[$roomId]"))
        participants -= name
      case msg: IncomingMessage =>
        if(msg.iText == "no") participants -= msg.user
        broadcast(msg)

    }

    def broadcast(msg : ChatMessage) : Unit = participants.values.foreach(_ ! msg)
  }

  Http().bindAndHandle(route,"localhost",9090)



}
