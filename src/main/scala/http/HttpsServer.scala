package http

import java.security.{KeyStore, SecureRandom}
import java.util.{MissingFormatArgumentException, UUID}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

import akka.actor.ActorSystem
import akka.http.javadsl.model.headers.RawHeader
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.{ConnectionContext, Http}
import akka.http.scaladsl.model.{headers, _}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.headers.{Location, RawHeader}
import akka.stream.ActorMaterializer
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.RejectionHandler
import akka.http.scaladsl.unmarshalling.{FromRequestUnmarshaller, FromResponseUnmarshaller}

import spray.json._
import javax.net.ssl.{KeyManagerFactory, SSLContext, TrustManagerFactory}

object HttpsServer extends App with DefaultJsonProtocol with SprayJsonSupport {

  val keyStore : KeyStore = KeyStore.getInstance("PKCS12")

  val keyStoreFile = getClass.getClassLoader.getResourceAsStream("keystore.pkcs12")

  val password = "akka-https".toCharArray

  keyStore.load(keyStoreFile,password)

  val keyManagerFactory = KeyManagerFactory.getInstance("SunX509")
  keyManagerFactory.init(keyStore,password)

  val trustManagerFactory = TrustManagerFactory.getInstance("SunX509")
  trustManagerFactory.init(keyStore)

  val sslContext = SSLContext.getInstance("TLS")

  sslContext.init(keyManagerFactory.getKeyManagers,trustManagerFactory.getTrustManagers,new SecureRandom)

  val httpConnectionContext = ConnectionContext.https(sslContext)


  case class Person(name : String , age : Int)
  case class Peoples(peoples: List[Person])
  val people = ( 1 to 10).map(x => Person( x.toString, x)).toList
  val peoples = Peoples(people)

  implicit val peopleFormat = jsonFormat2(Person)
  implicit val system       = ActorSystem("Https")
  implicit val materializer = ActorMaterializer()
//  val requestHandler : HttpRequest => HttpResponse = {
//    case HttpRequest(HttpMethods.GET, _, _, _, _) =>
//      HttpResponse(
//        entity = HttpEntity(
//          contentType = ContentTypes.`text/html(UTF-8)`,
//          string        =
//            """
//              |<html>
//              |<body>
//              |Your https website is up
//              |</body>
//              |</html>
//            """.stripMargin
//        )
//      )
//
//    case req : HttpRequest =>
//      req.discardEntityBytes()
//      HttpResponse(
//        status = StatusCodes.NotFound,
//        entity = HttpEntity(
//        )
//      )
//  }

  val badHandlers : RejectionHandler = { rejection =>

   println(rejection.head)
    Some(redirect("/route",StatusCodes.PermanentRedirect))
  }
  val peopleFuture = Future(people)
  val routes =
    handleRejections(badHandlers) {
      path("home"){
        get {
          parameter('id.as[Int]){id  =>
            complete("Hello")
          }
        } ~
          post {
            entity(implicitly[FromRequestUnmarshaller[Person]]){ p =>
              complete("Hello")
            }
          }
      } ~
        path("forbidden"){

          post {
            complete("Hello")
          }
        } ~ path("route") {
        complete(HttpResponse(
          StatusCodes.OK,
          entity = HttpEntity(
            ContentTypes.`text/html(UTF-8)`,
            "Redirected page"
          )
        ))
      }
    }

//
//  val rejectionHandler  = RejectionHandler.newBuilder()
//      .handle{
//        case ex : MissingFormatArgumentException
//      }

  Http().bindAndHandle(routes,"localhost",9092) onComplete{
    case Success (bdg) => println(s"Server is bound on ${bdg.localAddress}")
    case Failure(ex)   => println(s"Server cannot be bound because :  ${ex.getMessage}")
  }




}
