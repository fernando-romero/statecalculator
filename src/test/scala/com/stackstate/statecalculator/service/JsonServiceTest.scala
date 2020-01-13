package com.stackstate.statecalculator.service

import java.nio.file.{Path, Paths}

import akka.actor.ActorSystem
import com.stackstate.statecalculator.model._
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import tags.Integration

import scala.collection.immutable.Seq
import scala.concurrent.Await
import scala.concurrent.duration._

object JsonServiceTest {
  val componentsFile: Path = Paths.get("src/test/resources/sample-initial.json")
  val eventsFile: Path = Paths.get("src/test/resources/sample-events.json")

  private val checkStates: Map[String, State] = Map("CPU load" -> NoData, "RAM usage" -> NoData)

  val components = Components(Graph(Seq(
    Component("app", checkStates = checkStates, dependsOn = Some(Seq("db"))),
    Component("db", checkStates = checkStates, dependencyOf = Some(Seq("app"))),
  )))

  val events = Events(events = Seq(
    Event("1", "db", "CPU load", Warning),
    Event("2", "app", "CPU load", Clear)
  ))

  trait Context {
    val jsonService = new JsonService()
  }

}

@Integration
class JsonServiceTest extends AnyWordSpec with Matchers with BeforeAndAfterAll {

  import JsonServiceTest._
  import com.stackstate.statecalculator.json.JsonFormats._

  implicit val system: ActorSystem = ActorSystem()

  override def afterAll(): Unit = system.terminate()

  "Should read events from a file" in new Context {
    Await.result(jsonService.readEvents(eventsFile), 1.second) shouldBe events
  }

  "Should read components from a file" in new Context {
    Await.result(jsonService.readComponents(componentsFile), 1.second) shouldBe components
  }
}