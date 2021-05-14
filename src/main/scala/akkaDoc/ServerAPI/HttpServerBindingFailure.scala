// Akka HTTP サーバーの初期化や実行中には、さまざまな状況で障害が発生する可能性があります。
// Akka はデフォルトでこれらの障害をすべてログに記録しますが、アクターシステムをシャットダウンしたり、
// 外部の監視エンドポイントに明示的に通知したりするなど、ログに記録されるだけでなく障害に対応したい場合もあります。
//
// バインドの失敗
// たとえば、サーバーが指定されたポートにバインドできない場合があります。
// 例えば、そのポートが他のアプリケーションによって既に使用されている場合や、そのポートが特権的なものである場合（つまり、rootのみが使用可能）などです。
// この場合、「バインディング・フューチャー」は直ちに失敗し、フューチャーの完了を待ち受けることで対応することができます。

package akkaDoc.ServerAPI

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.server.Directives._

import scala.concurrent.{ExecutionContextExecutor, Future}

object HttpServerBindingFailure {
  def main(args: Array[String]): Unit = {
    implicit val system: ActorSystem = ActorSystem()
    // needed for the future foreach in the end
    implicit val executionContext: ExecutionContextExecutor = system.dispatcher

    val handler = get {
      complete("Hello world!")
    }

    // let's say the OS won't allow us to bind to 80.
    val (host, port) = ("localhost", 80)
    val bindingFuture: Future[ServerBinding] =
      Http().newServerAt(host, port).bindFlow(handler)

    // ここでFutureとして、ずっと待ってるのかな？？
    bindingFuture.failed.foreach { ex =>
      system.log.error(ex, "Failed to bind to {}:{}!", host, port)
    }
  }
}
