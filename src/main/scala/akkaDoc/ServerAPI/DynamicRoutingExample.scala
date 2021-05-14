// ルートはリクエストごとに評価されるため、実行時に変更することが可能です。
// すべてのアクセスは別のスレッドで行われる可能性があるため、共有される可変型ステートはスレッドセーフでなければならないことに注意してください。
//
// 以下は、実行時に関連するリクエスト・レスポンス・ペアを持つモックエンドポイントの新規追加や更新を動的に行うことができるAkka HTTPルートの定義です。

package akkaDoc.ServerAPI

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import spray.json.DefaultJsonProtocol._
import spray.json._

object DynamicRoutingExample {
  // いつも通りのケースクラス。
  case class MockDefinition(path: String, requests: Seq[JsValue], responses: Seq[JsValue])
  // それをjsonで受け取るためのimplicit
  implicit val format: RootJsonFormat[MockDefinition] = jsonFormat3(MockDefinition)

  // んあんなだこれ！
  // アノテーションしてる
  // どうやらマルチスレッドである状況で、キャッシュメモリーに格納されている変数などが、
  // 実メモリーへ格納されて各スレッド共通の値となる。
  // 「あるスレッドで更新された値が別スレッドで読み込まれる」ことが保証される。らしい。
  @volatile var state = Map.empty[String, Map[JsValue, JsValue]]

  // fixed route to update state
  val fixedRoute: Route = post {
    // ルートへのパス(https://heacet.com/ みたいなスラッシュ一つ)へマッチさせている
    pathSingleSlash {
      // マーシャリングしてる
      entity(as[MockDefinition]) { mock =>
        // collectionをペアで展開するzip
        val mapping = mock.requests.zip(mock.responses).toMap
        // タプル変換と、追加
        state = state + (mock.path -> mapping)
        complete("ok")
      }
    }
  }

  // dynamic routing based on current state
  // ctx: request context
  val dynamicRoute: Route = ctx => {
    // さっき展開して格納したやつをmapでそれぞれroutesに格納
    val routes = state.map {
      case (segment, responses) =>
        post {
          path(segment) {
            entity(as[JsValue]) { input =>
              complete(responses.get(input))
            }
          }
        }
    }
    // ここで入れる。_*によって、`routes.toList`で可変長引数を渡すことができる
    concat(routes.toList: _*)(ctx)
  }

  // ~ で notをとる
  val route: Route = fixedRoute ~ dynamicRoute
}
