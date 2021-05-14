// アップロードを処理するための高レベルのディレクティブについては、FileUploadDirectivesを参照してください。
//
// ファイル入力のあるブラウザフォームなどからの単純なファイルアップロードを処理するには、Multipart.FormData エンティティを受け入れることで実現できますが、
// ボディパーツはすぐにはすべて利用できないソースであり、個々のボディパーツのペイロードも同様であることに注意してください。
//
// ここでは、アップロードされたファイルをディスク上の一時ファイルにダンプし、いくつかのフォームフィールドを収集し、架空のデータベースにエントリを保存するだけの簡単な例を示します。

package akkaDoc.ServerAPI

import java.io.File
import akka.Done
import akka.actor.ActorRef
import akka.http.impl.util.StreamUtils.OnlyRunInGraphInterpreterContext.system
import akka.http.scaladsl.model.Multipart.FormData.BodyPart
import akka.stream.scaladsl.Framing
import akka.stream.scaladsl._
import akka.http.scaladsl.model.Multipart
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.RoutingSpec
import akka.util.ByteString
import akkaDoc.chap1.SprayJsonExample.system

import scala.concurrent.duration._
import scala.concurrent.Future

class FileUploadExamplesSpec extends RoutingSpec with CompileOnlySpec {

  // いつもの入力ケースクラス。
  case class Video(file: File, title: String, author: String)
  // なんかオブジェクトしてる
  object db {
    def create(video: Video): Future[Unit] = Future.successful(())
  }

  "simple-upload" in {
    //#simple-upload
    val uploadVideo =
      path("video") {
        entity(as[Multipart.FormData]) { formData =>

          // collect all parts of the multipart as it arrives into a map
          val allPartsF: Future[Map[String, Any]] = formData.parts.mapAsync[(String, Any)](1) {

            case b: BodyPart if b.name == "file" =>
              // stream into a file as the chunks of it arrives and return a future
              // file to where it got stored
              val file = File.createTempFile("upload", "tmp")
              b.entity.dataBytes.runWith(FileIO.toPath(file.toPath)).map(_ =>
                (b.name -> file))

            case b: BodyPart =>
              // collect form field values
              b.toStrict(2.seconds).map(strict =>
                (b.name -> strict.entity.data.utf8String))

          }.runFold(Map.empty[String, Any])((map, tuple) => map + tuple)

          val done = allPartsF.map { allParts =>
            // You would have some better validation/unmarshalling here
            db.create(Video(
              file = allParts("file").asInstanceOf[File],
              title = allParts("title").asInstanceOf[String],
              author = allParts("author").asInstanceOf[String]))
          }

          // when processing have finished create a response for the user
          onSuccess(allPartsF) { allParts =>
            complete {
              "ok!"
            }
          }
        }
      }
    //#simple-upload
  }

  // アップロードされたファイルを、前の例のように一時ファイルに保存するのではなく、届いた時点で変換することができます。
  // この例では、任意の数の .csv ファイルを受け入れ、それらを行に解析し、各行を分割してからアクターに送信して処理を行います。

  object MetadataActor {
    case class Entry(id: Long, values: Seq[String])
  }
  val metadataActor: ActorRef = system.deadLetters

  "stream-csv-upload" in {
    //#stream-csv-upload
    val splitLines = Framing.delimiter(ByteString("\n"), 256)

    val csvUploads =
      path("metadata" / LongNumber) { id =>
        entity(as[Multipart.FormData]) { formData =>
          val done: Future[Done] = formData.parts.mapAsync(1) {
            case b: BodyPart if b.filename.exists(_.endsWith(".csv")) =>
              b.entity.dataBytes
                .via(splitLines)
                .map(_.utf8String.split(",").toVector)
                .runForeach(csv =>
                  metadataActor ! MetadataActor.Entry(id, csv))
            case _ => Future.successful(Done)
          }.runWith(Sink.ignore)

          // when processing have finished create a response for the user
          onSuccess(done) { _ =>
            complete {
              "ok!"
            }
          }
        }
      }
    //#stream-csv-upload
  }

}