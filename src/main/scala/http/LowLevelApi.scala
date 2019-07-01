package http

import scala.concurrent.Future
import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.IncomingConnection
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.Location
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink

object LowLevelApi extends App {

  implicit val system       = ActorSystem()
  implicit val materializer = ActorMaterializer()

  val connectionSource = Http().bind("localhost",9090)

  val requestHandler : HttpRequest => HttpResponse = {
    case HttpRequest(HttpMethods.GET, _, _, _, _) =>
      HttpResponse(
        status = StatusCodes.OK,
        entity = HttpEntity(
          contentType = ContentTypes.`text/html(UTF-8)`,
          string =
            """
              |<html>
              |<body>
              |Hello from akka Http Api
              |</body>
              |</html>
            """.stripMargin
        )
      )

    case req : HttpRequest =>
      req.discardEntityBytes()
      HttpResponse(
        status = StatusCodes.NotFound,
        entity = HttpEntity(
          contentType = ContentTypes.`text/html(UTF-8)`,
          string =
            """
              |<html>
              |<body>
              |Resource not Found
              |</body>
              |</html>
            """.stripMargin
        )
      )
  }

  val asyncRequestHandler : HttpRequest => Future[HttpResponse] = {
    case HttpRequest(HttpMethods.GET, Uri.Path("/home") , _, _, _) =>
      Future(
        HttpResponse(
          status = StatusCodes.OK,
          entity = HttpEntity(
            contentType = ContentTypes.`text/html(UTF-8)`,
            string =
              """
                |<html>
                |<body>
                |Home Path
                |</body>
                |</html>
              """.stripMargin
          )
        )
      )

//    case HttpRequest(HttpMethods.GET, _ , _, _, _) =>
//      Future(
//        HttpResponse(
//          status = StatusCodes.OK,
//          entity = HttpEntity(
//            contentType = ContentTypes.`text/html(UTF-8)`,
//            string =
//              """
//                |<html>
//                |<body>
//                |<h1>Base Path</h1>
//                |</body>
//                |</html>
//              """.stripMargin
//          )
//        )
//      )

    case req : HttpRequest =>
      Future(
        HttpResponse(
          status = StatusCodes.Found,
          headers = List(Location("/home"))
          )
        )

  }



  def path(path : String)(f : String => HttpResponse) = ???

  connectionSource.to(Sink.foreach[IncomingConnection](conn =>
  conn.handleWithAsyncHandler(asyncRequestHandler)
  )).run() onComplete{
    case Success(bdg) => println(s"server bound on ${bdg.localAddress}")
    case Failure(ex)  => println(s"server could not be bound because : ${ex.getMessage}")
  }


}
