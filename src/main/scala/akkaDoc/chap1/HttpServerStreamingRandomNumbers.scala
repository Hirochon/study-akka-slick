// Akka HTTPの強みの1つは、ストリーミングデータがその中核にあることです。
// つまり、リクエストボディとレスポンスボディの両方がサーバーを介してストリーミングされ、
// 非常に大きなリクエストやレスポンスであっても一定のメモリ使用量を実現します。
//
// ストリーミング・レスポンスは、リモート・クライアントによってバック・プレッシャーがかけられ、
// サーバーがクライアントの処理能力を超えてデータをプッシュすることはありません。
//
// クライアントが乱数を受け入れる限り、乱数をストリーミングする例。

package akkaDoc.chap1

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.stream.scaladsl._
import akka.util.ByteString
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives._

import scala.concurrent.ExecutionContextExecutor
import scala.util.Random
import scala.io.StdIn


object HttpServerStreamingRandomNumbers {
  def main(args: Array[String]): Unit = {

    implicit val system: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "RandomNumbers")
    // needed for the future flatMap/onComplete in the end
    implicit val executionContext: ExecutionContextExecutor = system.executionContext

    // streams are re-usable so we can define it here　ストリームは再利用可能なので、ここで定義することができます。
    // and use it for every request　そして、すべてのリクエストにそれを使用する
    val numbers = Source.fromIterator(() =>
      Iterator.continually(Random.nextInt()))

    val route =
      path("random") {
        get {
          complete(
            HttpEntity(
              ContentTypes.`text/plain(UTF-8)`,
              // transform each number to a chunk of bytes 各数値をバイトの塊に変換
              numbers.map(n => ByteString(s"$n\n"))
            )
          )
        }
      }

    val bindingFuture = Http().newServerAt("localhost", 8080).bind(route)
    println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
    StdIn.readLine() // let it run until user presses return
    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ => system.terminate()) // and shutdown when done
  }
}
