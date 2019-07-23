package http.websockets

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.stream.{ActorMaterializer, FlowShape, OverflowStrategy}
import akka.http.scaladsl.server._
import akka.http.scaladsl.server.Directives._
import akka.stream.scaladsl.{Flow, GraphDSL, Merge, Sink, Source}



object Ads extends App {

  implicit val system = ActorSystem("WebSockets")
  implicit val materializer = ActorMaterializer()

  //define the route for the websocket

  val routes  =
    path("wschat" / IntNumber){ chatRoomId =>
      parameter('name){ username =>
        handleWebSocketMessages(ChatRooms.findOrCreate(chatRoomId).websocketFlow(username))
      }

    }

  Http().bindAndHandle(routes,"localhost",9090)

}

sealed trait ChatEvent
case class ChatMessage(author : String, text : String) extends ChatEvent
case class IncomingMessage(author : String, text : String) extends ChatEvent
case class UserJoined(user : String,actor : ActorRef) extends ChatEvent
case class UserLeft(user : String)

class ChatRoom(id : Int, system : ActorSystem){
  private[this] val chatRoomActor = system.actorOf(Props(new ChatRoomActor(id)),s"$id")

  def websocketFlow(user: String): Flow[Message, Message, _] =
    Flow.fromGraph(GraphDSL.create(Source.actorRef[ChatMessage](bufferSize = 20, OverflowStrategy.fail)) {
      implicit builder =>
        userAsActor =>
          import GraphDSL.Implicits._

          //the input of this is what is connected to the websocket and receives the response from the
          val fromWebSocket = builder.add(Flow[Message].map {
            case TextMessage.Strict(txt) => IncomingMessage(user, txt)
          })
          //the output of this is sent back to the client
          val toWebSocket = builder.add(Flow[ChatMessage].map {
            case ChatMessage(auth, txt) => TextMessage(s"[$auth] : $txt")
          })

          //for each connection, an actor uis created, and its materialized value is mapped over to get the UserJoined Case class
          val materializedActor = builder.materializedValue.map(actor => UserJoined(user, actor))

          val merge = builder.add(Merge[ChatEvent](2))
          val sinkActorRef = Sink.actorRef(chatRoomActor, UserLeft(user))

          //all messages from the client after he has connected
          fromWebSocket ~> merge

          //the singular message when this client connects
          materializedActor ~> merge

          //send all messages to the chatroom actor
          merge ~> sinkActorRef

          //all received messages from the chatActor (messages that were broadcasted) send to back to the webclient
          userAsActor ~> toWebSocket

          //resulting flow shape
          FlowShape(fromWebSocket.in, toWebSocket.out)
    }

    )
}
object ChatRoom{
  def apply(roomId : Int )(implicit system : ActorSystem) : ChatRoom = {
    new ChatRoom(roomId,system)
  }
}

object ChatRooms {
  private var chatRooms = Map.empty[Int,ChatRoom]
  def findOrCreate(id : Int)(implicit system : ActorSystem) = chatRooms.getOrElse(id,createNewChatId(id))

  def createNewChatId(i: Int)(implicit system : ActorSystem) = {
    val chatRoom = ChatRoom(i)
    chatRooms +=  i -> chatRoom
    chatRoom
  }

}

class ChatRoomActor(id : Int) extends Actor {

  private var participants = Map.empty[String,ActorRef]
  override def receive: Receive = {

    case UserJoined(user,actor) =>
      println(s"[$user] joined the chatroom [$id] ")
      broadCast(ChatMessage(user, "just joined the channel"))
      participants += user -> actor

    case UserLeft(user) =>
      broadCast(ChatMessage(user, "just left the channel"))
      participants -= user


    case  IncomingMessage(author , text) =>
      broadCast(ChatMessage(author, text))

  }
  def broadCast(msg : ChatMessage) = participants.values.foreach(_ ! msg)
}
