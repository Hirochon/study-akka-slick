// 続いて、HTTPルートのJSONマーシャラーとアンマーシャラーを定義

package akkaDoc.ServerAPI

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.DefaultJsonProtocol
import spray.json.DeserializationException
import spray.json.JsString
import spray.json.JsValue
import spray.json.RootJsonFormat

trait JsonSupport extends SprayJsonSupport {
  // import the default encoders for primitive types (Int, String, Lists etc)
  // プリミティブタイプ（Int、String、Listsなど）のデフォルトエンコーダのインポート
  import DefaultJsonProtocol._
  import JobRepository._

  implicit object StatusFormat extends RootJsonFormat[Status] {
    def write(status: Status): JsValue = status match {
      case Failed     => JsString("Failed")   // JobRepositoryで定義してたcase class。
      case Successful => JsString("Successful")   // JobRepositoryで定義してたcase class。
    }

    def read(json: JsValue): Status = json match {
      case JsString("Failed")     => Failed   // JobRepositoryで定義してたcase class。
      case JsString("Successful") => Successful   // JobRepositoryで定義してたcase class。
      case _                      => throw new DeserializationException("Status unexpected")
    }
  }

  // final case class Job(id: Long, projectName: String, status: Status, duration: Long)
  implicit val jobFormat: RootJsonFormat[Job] = jsonFormat4(Job)
}
