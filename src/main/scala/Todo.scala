/*
 * Copyright (C) 2020-2021 Lightbend Inc. <https://www.lightbend.com>
 */

//package docs.http.scaladsl

import slick.jdbc.MySQLProfile.api._
import spray.json.RootJsonFormat

import scala.concurrent.{Await, ExecutionContextExecutor}
import scala.concurrent.duration.Duration

//import scala.concurrent.ExecutionContext.Implicits.global

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.Done
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
//import akka.http.scaladsl.model.StatusCodes
// for JSON serialization/deserialization following dependency is required:
// "com.typesafe.akka" %% "akka-http-spray-json" % "10.1.7"
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import spray.json.DefaultJsonProtocol._

import scala.io.StdIn

import scala.concurrent.Future

import TodoInit.TodoList

object Todo {
  val todoList = TableQuery[TodoList]

  val db = Database.forConfig(path="myDB")

  // needed to run the route
  implicit val system: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "SprayExample")
  // needed for the future map/flatmap in the end and future in fetchItem and saveOrder
  implicit val executionContext: ExecutionContextExecutor = system.executionContext

  // domain model
  final case class Item(todo: String)
  final case class ItemList(items: List[Item])
  final case class FetchItem(id:Int, todo: String)
  final case class DeleteItem(id: Int)

  var itemList: List[Item] = Nil

  // formats for unmarshalling and marshalling
  implicit val itemFormat: RootJsonFormat[Item] = jsonFormat1(Item)
  implicit val orderFormat: RootJsonFormat[ItemList] = jsonFormat1(ItemList)
  implicit val updateFormat: RootJsonFormat[FetchItem] = jsonFormat2(FetchItem)
  implicit val deleteFormat: RootJsonFormat[DeleteItem] = jsonFormat1(DeleteItem)

  def saveItemList(list: ItemList): Future[Done] = {
    list match {
      case ItemList(items) =>
        val setup = DBIO.seq(todoList += (0, items.head.todo))

        val setupFuture = db.run(setup)
        Await.result(setupFuture, Duration.Inf)
      case _            => println("not match")
    }
    Future { Done }
  }

  def fetchItem: Future[Seq[(Int, String)]] = db.run {
    todoList.result
  }

  def updateItem(fetchItem: FetchItem): Future[Done] = {
    val query = for { item <- todoList if item.id === fetchItem.id} yield item.todo
    val updateAction = query.update(fetchItem.todo)
    val updateFuture = db.run(updateAction)
    Await.result(updateFuture, Duration.Inf)

//    val sql = query.updateStatement
//    println(sql)

    Future { Done }
  }

  def deleteItem(fetchItem: DeleteItem): Future[Done] = {
    val query = todoList.filter(_.id === fetchItem.id)
    val deleteAction = query.delete
    val deleteFuture = db.run(deleteAction)
    Await.result(deleteFuture, Duration.Inf)

    val sql = deleteAction.statements.head
    println(sql)

    Future { Done }
  }

  def main(args: Array[String]): Unit = {
    val route: Route =
      concat(
        post {
          path("create-todo") {
            entity(as[ItemList]) { itemList =>
              val saved: Future[Done] = saveItemList(itemList)
              onSuccess(saved) { _ => // we are not interested in the result value `Done` but only in the fact that it was successful
                complete("todo created")
              }

            }
          }
        },
        get {
          path("todo-list") {
            val fetched: Future[Seq[(Int, String)]] = fetchItem
            onSuccess(fetched){ item => complete(item)}
          }
        },
        put {
          path("update-todo") {
            entity(as[FetchItem]) { item =>
              val saved: Future[Done] = updateItem(item)
              onSuccess(saved) { _ => // we are not interested in the result value `Done` but only in the fact that it was successful
                complete("todo updated!")
              }
            }
          }
        },
        delete {
          path("delete-todo") {
            entity(as[DeleteItem]) { item =>
              val saved: Future[Done] = deleteItem(item)
              onSuccess(saved) { _ => // we are not interested in the result value `Done` but only in the fact that it was successful
                complete("todo deleted!")
              }
            }
          }
        }
      )

      val bindingFuture = Http().newServerAt("localhost", 8080).bind(route)
      println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
      StdIn.readLine() // let it run until user presses return
      try {
        bindingFuture
          .flatMap(_.unbind()) // trigger unbinding from the port
          .onComplete(_ => system.terminate()) // and shutdown when done
      } finally db.close
  }
}