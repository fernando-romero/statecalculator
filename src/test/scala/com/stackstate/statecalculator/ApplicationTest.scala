package com.stackstate.statecalculator

import java.nio.file.{Path, Paths}

import akka.actor.{ActorSystem, Terminated}
import com.stackstate.statecalculator.model._
import com.stackstate.statecalculator.service.{ComponentService, JsonService, StateService}
import com.stephenn.scalatest.jsonassert.JsonMatchers
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import spray.json._

import scala.collection.immutable.Seq
import scala.collection.mutable
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

object ApplicationTest {

  val events: Seq[Event] = Seq(
    Event("3", "app", "RAM usage", Warning),
    Event("1", "db", "CPU load", Warning),
    Event("2", "app", "CPU load", Clear)
  )

  val components: Seq[Component] = Seq(
    Component("app"),
    Component("db")
  )

  class JsonServiceMock(files: mutable.Queue[Path]) extends JsonService {
    override def readComponents(file: Path)(implicit reader: JsonReader[Components], system: ActorSystem)
    : Future[Components] = {
      files.enqueue(file)
      Future.successful(Components(Graph(components)))
    }

    override def readEvents(file: Path)(implicit reader: JsonReader[Events], system: ActorSystem)
    : Future[Events] = {
      files.enqueue(file)
      Future.successful(Events(events))
    }
  }

  class StateServiceMock(componentService: ComponentService, events: mutable.Queue[Event]) extends StateService(componentService) {
    override def processEvent(event: Event): Unit = {
      events.enqueue(event)
      // Not calling super here, StateService behaviour is tested in its own test.
    }
  }

  trait Context {
    val filesSpy: mutable.Queue[Path] = mutable.Queue[Path]()
    val eventsSpy: mutable.Queue[Event] = mutable.Queue[Event]()

    private val jsonService = new JsonServiceMock(filesSpy)
    private val componentService = new ComponentService()
    private val stateService = new StateServiceMock(componentService, eventsSpy)

    val application = new Application(jsonService, componentService, stateService)
  }

}

class ApplicationTest extends AnyWordSpec with Matchers with JsonMatchers {

  import ApplicationTest._
  import com.stackstate.statecalculator.json.JsonFormats._

  "run should get the components, process events in order and return the final graph" in new Context {
    private val json = Await.result(application.run("state.json", "events.json"), 1.second)

    filesSpy.dequeue() shouldBe Paths.get("state.json")
    filesSpy.dequeue() shouldBe Paths.get("events.json")

    eventsSpy.dequeue().timestamp shouldBe "1"
    eventsSpy.dequeue().timestamp shouldBe "2"
    eventsSpy.dequeue().timestamp shouldBe "3"

    json should matchJson(Components(Graph(components)).toJson.prettyPrint)
  }

  "shutdown should terminate the actor system" in new Context {
    application.shutdown()
    Await.result(application.system.whenTerminated, 1.second) shouldBe a[Terminated]
  }
}
