import slick.jdbc.MySQLProfile.api._

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object TodoInit extends App{
  class TodoList(tag: Tag) extends Table[(Int, String)](tag, _tableName="todo_list") {
    def id: Rep[Int] = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def todo: Rep[String] = column[String]("todo")
    def * = (id, todo)
  }

  val todoList = TableQuery[TodoList]

  val db = Database.forConfig(path="myDB")
  try {
    val setup = DBIO.seq(todoList.schema.create)

    val setupFuture = db.run(setup)
    Await.result(setupFuture, Duration.Inf)
  } finally db.close
}
