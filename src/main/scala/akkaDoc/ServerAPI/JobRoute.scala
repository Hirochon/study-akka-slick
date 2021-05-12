// 次のステップは、先に定義したビヘイビアと通信し、その可能なすべてのレスポンスを処理するRouteを定義することです。

package akkaDoc.ServerAPI

import akka.actor.typed.{ActorRef, ActorSystem}
import akka.util.Timeout
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route

import scala.concurrent.duration._
import scala.concurrent.Future


class JobRoute(buildJobRepository: ActorRef[JobRepository.Command])(implicit system: ActorSystem[_]) extends JsonSupport {
  import akka.actor.typed.scaladsl.AskPattern.schedulerFromActorSystem
  import akka.actor.typed.scaladsl.AskPattern.Askable

  // asking someone requires a timeout and a scheduler, if the timeout hits without response
  // the ask is failed with a TimeoutException
  // 誰かに質問をするには、タイムアウトとスケジューラーが必要です。
  // タイムアウトが過ぎても応答がない場合、質問はTimeoutExceptionで失敗します。
  implicit val timeout: Timeout = 3.seconds

  lazy val theJobRoutes: Route = {
    // pathとは違う。
    // 与えられた PathMatcher に対して、RequestContext のマッチしなかったパスのプレフィックスをマッチさせて消費し、
    // (引数のタイプに応じて) 一つ以上の値を抽出する可能性があります。
    // スラッシュは自動的に追加
    pathPrefix("jobs") {
      // 連結する
      concat(
        // RequestContextのマッチしていないパスが空である場合、
        // つまり、リクエストのパスがより上位のpathまたはpathPrefixディレクティブによって完全にマッチしている場合にのみ、
        // リクエストを内部のルートに渡します。
        pathEnd {
          // 連結する
          concat(
            post {
              // シリアライズ(アンマーシャル)
              // アンマーシャルで成功したjobを渡す。
              // 成功しないという渡し方もある https://doc.akka.io/docs/akka-http/current/routing-dsl/directives/marshalling-directives/entity.html
              entity(as[JobRepository.Job]) { job =>
                // sealed trait Response
                // case object OK extends Response
                // final case class KO(reason: String) extends Response
                val operationPerformed: Future[JobRepository.Response] = {
                  // case AddJob(job, replyTo) if jobs.contains(job.id) =>
                  //    replyTo ! KO("Job already exists")
                  //    Behaviors.same
                  //  case AddJob(job, replyTo) =>
                  //    replyTo ! OK
                  //    JobRepository(jobs.+(job.id -> job))
                  buildJobRepository.ask(JobRepository.AddJob(job, _))
                }
                // おもろい！！！！
                onSuccess(operationPerformed) {
                  case JobRepository.OK         => complete("Job added")
                  case JobRepository.KO(reason) => complete(StatusCodes.InternalServerError -> reason)
                }
              }
            },
            delete {
              val operationPerformed: Future[JobRepository.Response] =
                buildJobRepository.ask(JobRepository.ClearJobs)
              onSuccess(operationPerformed) {
                case JobRepository.OK         => complete("Jobs cleared")
                case JobRepository.KO(reason) => complete(StatusCodes.InternalServerError -> reason)
              }
            }
          )
        },
        // ネストさせる代わりに&演算子で短縮
        (get & path(LongNumber)) { id =>
          val maybeJob: Future[Option[JobRepository.Job]] =
            buildJobRepository.ask(JobRepository.GetJobById(id, _))
          rejectEmptyResponse {
            complete(maybeJob)
          }
        }
      )
    }
  }
}
