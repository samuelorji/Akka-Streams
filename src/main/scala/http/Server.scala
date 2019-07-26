package http

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._

object Server extends App {

  implicit val system       = ActorSystem("Https")
  implicit val materializer = ActorMaterializer()


}
