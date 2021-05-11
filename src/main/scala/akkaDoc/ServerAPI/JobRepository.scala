// 次の例では、Akka HTTP を Akka Actor と共に使用する方法を示しています。
//
// ここでは、ビルドジョブの状態と期間を記録し、ID とステータスでジョブを照会し、ジョブの履歴を消去する役割を持つ小さな Web サーバーを作成します。
//
// まず、ビルドジョブ情報のリポジトリとして機能するBehaviorの定義から始めましょう。
// これは今回のサンプルでは厳密には必要ではありませんが、実際のアクターとのやり取りをするために必要です。

package akkaDoc.ServerAPI

import akka.actor.typed.{ ActorRef, Behavior }
import akka.actor.typed.scaladsl.Behaviors

object JobRepository {
  // Definition of the a build job and its possible status values
  // ビルドジョブの定義とその可能なステータス値
  // sealedはこのファイルでしか扱えない。
  sealed trait Status
  object Successful extends Status
  object Failed extends Status

  // final修飾子はサブクラスを持つことを許さないらしい
  final case class Job(id: Long, projectName: String, status: Status, duration: Long)

  // Trait defining successful and failure responses
  // 成功と失敗のレスポンスを定義するTrait
  sealed trait Response
  case object OK extends Response
  final case class KO(reason: String) extends Response

  // Trait and its implementations representing all possible messages that can be sent to this Behavior
  // このBehaviorに送ることができるすべての可能なメッセージを表すTraitとその実装
  // ActorRefは、Actorインスタンスの IDやアドレス。またBehaviorsのスーパークラスでもある。
  sealed trait Command
  final case class AddJob(job: Job, replyTo: ActorRef[Response]) extends Command
  final case class GetJobById(id: Long, replyTo: ActorRef[Option[Job]]) extends Command
  final case class ClearJobs(replyTo: ActorRef[Response]) extends Command

  // This behavior handles all possible incoming messages and keeps the state in the function parameter
  // この動作は、可能な限りの受信メッセージを処理し、状態を関数パラメータに保持します。
  //
  // receiveMessage(msg: T)。ビヘイビア[T]
  // 受信したメッセージを処理し、次のビヘイビアを返します。
  // 返されたビヘイビアは、通常のビヘイビアに加えて、以下のような定型の特殊オブジェクトの1つにすることができます。
  // *stoppedを返すと、このビヘイビアを終了します
  // *sameを返すと、現在のビヘイビアを再利用します
  // *unhandledを返すと、同じビヘイビアを維持し、メッセージがまだ処理されていないことを通知します
  def apply(jobs: Map[Long, Job] = Map.empty): Behavior[Command] = Behaviors.receiveMessage {
    case AddJob(job, replyTo) if jobs.contains(job.id) =>
      replyTo ! KO("Job already exists")
      // Getとaddにミスった時はbehaviorsが変わらないからsame渡してんのかな？
      Behaviors.same
    case AddJob(job, replyTo) =>
      replyTo ! OK
      // 確かに型があってるわ！
      // applyの受ける型がMap[Long, Job]で
      // その状態を補償しとるjobsにtuple(job.id, job)で追加してる。
      JobRepository(jobs.+(job.id -> job))
    case GetJobById(id, replyTo) =>
      replyTo ! jobs.get(id)
      // Getとaddにミスった時はbehaviorsが変わらないからsame渡してんのかな？
      Behaviors.same
    case ClearJobs(replyTo) =>
      replyTo ! OK
      // 空にしてるのがわかる
      JobRepository(Map.empty)
  }
}
