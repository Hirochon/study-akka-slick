import slick.ast.BaseTypedType
import slick.jdbc.JdbcType
import slick.jdbc.MySQLProfile.api._
import slick.sql.SqlProfile.ColumnOption.SqlType

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.language.implicitConversions

object PlanTableInit extends App{

//  val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
//  implicit def LocalDateTimeToStr(d: LocalDateTime): String = {
//    d.format(formatter)
//  }
//  implicit def strToLocalDateTime(s: String): LocalDateTime = {
//    LocalDateTime.parse(s, formatter)
//  }
//  implicit val localDateTimeMapper: JdbcType[LocalDateTime] with BaseTypedType[LocalDateTime] = MappedColumnType.base[LocalDateTime, String](
//    LocalDateTimeToStr,
//    strToLocalDateTime
//  )

  class PlanTable(tag: Tag) extends Table[(Int, String, Int, String, Int, String)](tag, _tableName="plan") {
    def id: Rep[Int] = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def todo: Rep[String] = column[String]("todo", SqlType("VARCHAR(256)"))
    def urgency: Rep[Int] = column[Int]("urgency")
    def deadline: Rep[String] = column[String]("deadline", O.SqlType("datetime"))
    def status: Rep[Int] = column[Int]("status")
    def createdAt: Rep[String] = column[String]("created_at", O.SqlType("datetime"))
    def * = (id, todo, urgency, deadline, status, createdAt)
  }

  val todoList = TableQuery[PlanTable]

  val db = Database.forConfig(path="myDB")
  try {
    val setup = DBIO.seq(todoList.schema.create)

    val setupFuture = db.run(setup)
    Await.result(setupFuture, Duration.Inf)
  } finally db.close
}
