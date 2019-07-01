package http

import java.io.File

import scala.concurrent.Future
import scala.util.{Failure, Success}

import akka.Done
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.Multipart.FormData
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.Directives._
import akka.stream.scaladsl.{FileIO, Flow, Sink, Source}
import akka.stream.{ActorMaterializer, IOResult}
import akka.util.{ByteString, Timeout}

import spray.json._

object UploadingFiles extends App with DefaultJsonProtocol with SprayJsonSupport {


  implicit val system = ActorSystem("UploadingFiles")
  implicit val materializer = ActorMaterializer()

  import scala.concurrent.ExecutionContext.Implicits.global

  val routes =
    (pathEndOrSingleSlash & get) {
      complete(
        HttpEntity(
          ContentTypes.`text/html(UTF-8)`,
          """
            |<html>
            |  <body>
            |    <form action="/upload" method="post" enctype="multipart/form-data">
            |      <input type="file" name="myFil">
            |      <button type="submit">Upload</button>
            |    </form>
            |  </body>
            |</html>
          """.stripMargin
        )
      )
    } ~
      (path("upload") & extractLog) { log =>
        post {

          entity(as[Multipart.FormData]) { formdata =>
            val partsSource: Source[FormData.BodyPart, Any] = formdata.parts //Get The bodyPart from the file Data
            val partsFlow   = Flow[Multipart.FormData.BodyPart].map { fileData => {
              if (fileData.name == "myFile") {
                val filename = "./data/" + fileData.filename.getOrElse("tempFile_" + System.currentTimeMillis())
                val file = new File(filename)
                val fileContentsSource = fileData.entity.dataBytes
                Left(fileContentsSource, file)
              } else {
                Right(new RuntimeException("Unexpected File"))
              }
             }
            }
            val fileSink = Sink.fold[Future[IOResult], Either[(Source[ByteString, Any], File), RuntimeException]](Future(new IOResult(1L, Success(Done)))) {
              case (_, Left((src, file))) =>
                src.runWith(FileIO.toPath(file.toPath))

              case (_, Right(ex)) =>
                Future(new IOResult(1L, Failure(ex)))

            }

            onComplete(partsSource.via(partsFlow).runWith(fileSink).flatten) {
              case Success(value) =>
                value match {
                  case res@IOResult(_, _) =>
                    if (res.wasSuccessful) complete("Successful Upload")
                    else {
                      log.error(s"Exception ${res.getError}")
                      complete("Unsuccessful Upload")
                    }
                }
              case Failure(ex) =>
                log.error(s"Exception Received $ex")
                complete("Unsuccessful Upload")
            }
          }
        }
    } ~
      (path("download") & extractLog) { log =>
      val file = new File("./data/file.jpg")
        if(file.exists()){
          respondWithHeaders(RawHeader("Content-Disposition",s"""attachment; filename="filename.jpg"""")) {
            complete(HttpEntity(ContentTypes.`application/octet-stream`, FileIO.fromPath(file.toPath)))
          }
        }else{
          log.error(s"File ${file.getName} does not exist")
          complete(StatusCodes.InternalServerError)
        }
    } ~
      path("error") {
      complete("Error page")
    }
  Http().bindAndHandle(routes,"localhost",9092)

}
