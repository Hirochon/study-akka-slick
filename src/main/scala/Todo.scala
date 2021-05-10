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

import PlanTableInit.PlanTable

object Todo {
  val planTable = TableQuery[PlanTable]

  val db = Database.forConfig(path="myDB")

  // needed to run the route
  implicit val system: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "SprayExample")
  // needed for the future map/flatmap in the end and future in fetchItem and saveOrder
  implicit val executionContext: ExecutionContextExecutor = system.executionContext

  // domain model
  final case class Action(todo: String, urgency: Int, deadline: String, status: Int, createdAt: String)
  final case class GetAction(id: Int, todo: String, urgency: Int, deadline: String, status: Int, createdAt: String)
  final case class Plan(plan: Seq[GetAction])
  final case class FetchPlan(id:Int, todo: String)
  final case class DeleteAction(id: Int)

  // formats for unmarshalling and marshalling
  implicit val itemFormat: RootJsonFormat[Action] = jsonFormat5(Action)
  implicit val getActionFormat: RootJsonFormat[GetAction] = jsonFormat6(GetAction)
  implicit val orderFormat: RootJsonFormat[Plan] = jsonFormat1(Plan)
  implicit val updateFormat: RootJsonFormat[FetchPlan] = jsonFormat2(FetchPlan)
  implicit val deleteFormat: RootJsonFormat[DeleteAction] = jsonFormat1(DeleteAction)

  def saveAction(action: Action): Future[Done] = {
    action match {
      case Action(todo, urgency, deadline, status, createdAt) =>
        val setup = DBIO.seq(planTable += (0, todo, urgency, deadline, status, createdAt))

        val setupFuture = db.run(setup)
        Await.result(setupFuture, Duration.Inf)
      case _            => println("not match")
    }
    Future { Done }
  }

//  def fetchAction: Future[Option[Action]] = {
//    val planAction = db.run(planTable.result)
//    val plan = Await.result(planAction, Duration.Inf)
//    val preprocessedPlan: Plan = plan.toList
//    Future(Option(preprocessedPlan))
//  }
  def fetchAction: Future[Seq[GetAction]] = {
    val tmpPlanAction = db.run(planTable.result)
    val planAction = Await.result(tmpPlanAction, Duration.Inf)
    val plan = planAction.map(action =>
      GetAction(
        id = action._1,
        todo = action._2,
        urgency = action._3,
        deadline = action._4,
        status = action._5,
        createdAt = action._6,
    ))
    Future(plan)
  }

  def updateItem(fetchItem: FetchPlan): Future[Done] = {
    val query = for { item <- planTable if item.id === fetchItem.id} yield item.todo
    val updateAction = query.update(fetchItem.todo)
    val updateFuture = db.run(updateAction)
    Await.result(updateFuture, Duration.Inf)

//    val sql = query.updateStatement
//    println(sql)

    Future { Done }
  }

  def deleteItem(fetchItem: DeleteAction): Future[Done] = {
    val query = planTable.filter(_.id === fetchItem.id)
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
          path("create-action") {
            entity(as[Action]) { action =>
              val saved: Future[Done] = saveAction(action)
              onSuccess(saved) { _ => // we are not interested in the result value `Done` but only in the fact that it was successful
                complete("todo created!")
              }
            }
          }
        },
        get {
          path("todo-list") {
            val fetched: Future[Seq[GetAction]] = fetchAction
            // ここで並び替えとか噛ませたい
            onSuccess(fetched) { item =>
              complete(item)
            }
          }
        },
        put {
          path("update-todo") {
            entity(as[FetchPlan]) { item =>
              val saved: Future[Done] = updateItem(item)
              onSuccess(saved) { _ => // we are not interested in the result value `Done` but only in the fact that it was successful
                complete("todo updated!")
              }
            }
          }
        },
        delete {
          path("delete-todo") {
            entity(as[DeleteAction]) { item =>
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