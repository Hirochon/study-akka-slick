// Akka HTTP のハイレベルなルーティング API は、HTTP の「ルート」とその処理方法を記述する DSL を提供します。
// 各ルートは、1 つ以上のレベルの Directives で構成され、特定のタイプのリクエストを処理するように絞り込まれます。
//
// 例えば、あるルートは、リクエストのパスにマッチすることから始まり、それが「/hello」である場合にのみマッチし、
// 次にHTTP getリクエストのみを処理するように絞り込み、それらを文字列リテラルで完了させ、文字列をレスポンスボディとしてHTTP OKとして送り返すようにします。
//
// Route DSLを使って作成されたRouteは、HTTPリクエストの処理を開始するためのポートに「バインド」されます。

package akkaDoc.chap1

// ActorSystem一体何なんだろう
// アクター＝スレッド？
// 並列に別々に処理するためにあるのかな？
import akka.actor.typed.ActorSystem
// メッセージに対して反応としてとるべきアクションを定義する関数を意味する：Behavior!!!
import akka.actor.typed.scaladsl.Behaviors
// Http系の何か？
import akka.http.scaladsl.Http
// Akka HTTP モデルには、HTTP リクエスト、レスポンス、共通ヘッダーなど、
// すべての主要な HTTP データ構造の、深く構造化された、完全に不変の、ケースクラスベースのモデルが含まれています。
// このモデルは akka-http-core モジュールに含まれており、Akka HTTP のほとんどの API の基礎となっています。
// https://doc.akka.io/docs/akka-http/current/common/http-model.html
import akka.http.scaladsl.model._
// ディレクティブとは、任意の複雑なルート構造を作成するための小さな構成要素です。
// Akka HTTP はすでに多数のディレクティブを事前に定義しており、独自のディレクティブを簡単に構築することができます。
// https://doc.akka.io/docs/akka-http/current/routing-dsl/directives/index.html
import akka.http.scaladsl.server.Directives._

import scala.concurrent.ExecutionContextExecutor
import scala.io.StdIn

object HttpServerRoutingMinimal {

  def main(args: Array[String]): Unit = {

    // ActorSystemをOnにしてる
    implicit val system: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "my-system")
    // needed for the future flatMap/onComplete in the end
    implicit val executionContext: ExecutionContextExecutor = system.executionContext

    // Directiveより動いてると思う(Route)
    val route =
      path("hello") {
        get {
          complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "<h1>Say hello to akka-http</h1>"))
        }
      }

    val bindingFuture = Http().newServerAt("localhost", 8080).bind(route)

    println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
    StdIn.readLine() // let it run until user presses return ユーザーがリターンを押すまで実行させる
    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port ポートからのアンバインディングのトリガー
      .onComplete(_ => system.terminate()) // and shutdown when done 終わったらシャットダウン
  }
}
