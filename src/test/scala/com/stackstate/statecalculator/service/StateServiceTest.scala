package com.stackstate.statecalculator.service

import com.stackstate.statecalculator.model._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.collection.immutable.Seq

object StateServiceTest {
  val CPU_LOAD = "CPU load"
  val RAM_USAGE = "RAM usage"

  trait Context {
    private val componentService = new ComponentService()
    private val stateService = new StateService(componentService)
    private var eventCount = 1

    def withComponents(components: Component*): Unit = components.foreach(componentService.save)

    def withEvents(events: Event*): Unit = events.foreach(stateService.processEvent)

    def component(id: String): Component =
      componentService
        .findById(id)
        .getOrElse(throw new AssertionError(s"$id not found"))

    def event(id: String, checkState: String, state: State) = Event(eventCountUp, id, checkState, state)

    private def eventCountUp = {
      eventCount += 1
      eventCount.toString
    }
  }

}

class StateServiceTest extends AnyWordSpec with Matchers {

  import StateServiceTest._

  "When the component does not depend on other components and is no dependency to any other component" should {

    "update the own state using the highest of the check states" in new Context {
      withComponents(Component("app"))

      withEvents(event("app", CPU_LOAD, Clear))

      component("app").checkStates(CPU_LOAD) shouldBe Clear
      component("app").ownState shouldBe Clear

      withEvents(event("app", RAM_USAGE, Warning))

      component("app").checkStates(RAM_USAGE) shouldBe Warning
      component("app").ownState shouldBe Warning
    }

    "set the derived state to the value of the own state only if own state is Warning or superior" in new Context {
      withComponents(Component("app"))

      withEvents(event("app", CPU_LOAD, NoData))
      component("app").derivedState shouldBe NoData

      withEvents(event("app", CPU_LOAD, Clear))
      component("app").derivedState shouldBe NoData

      withEvents(event("app", CPU_LOAD, Warning))
      component("app").derivedState shouldBe Warning

      withEvents(event("app", CPU_LOAD, Alert))
      component("app").derivedState shouldBe Alert
    }

    "ignore events for unknown components" in new Context {
      private val app = Component("app")

      withComponents(app)
      withEvents(event("unknownApp", CPU_LOAD, Clear))

      component("app") shouldBe app
    }
  }

  "When there is an unidirectional dependency" should {

    // app -> db
    "propagate the derived change to the dependency (assignment example)" in new Context {
      private val checkStates = Map(CPU_LOAD -> NoData, RAM_USAGE -> NoData)

      withComponents(
        Component("app", checkStates = checkStates, dependsOn = Some(Seq("db"))),
        Component("db", checkStates = checkStates, dependencyOf = Some(Seq("app")))
      )

      withEvents(
        event("db", CPU_LOAD, Warning),
        event("app", CPU_LOAD, Clear)
      )

      component("app").checkStates(CPU_LOAD) shouldBe Clear
      component("app").checkStates(RAM_USAGE) shouldBe NoData
      component("app").ownState shouldBe Clear
      component("app").derivedState shouldBe Warning

      component("db").checkStates(CPU_LOAD) shouldBe Warning
      component("db").checkStates(RAM_USAGE) shouldBe NoData
      component("db").ownState shouldBe Warning
      component("db").derivedState shouldBe Warning
    }

    // app3 -> app2 -> app1
    "propagate the derived change in a chain of dependencies" in new Context {
      withComponents(
        Component("app1", dependencyOf = Some(Seq("app2"))),
        Component("app2", dependsOn = Some(Seq("app1")), dependencyOf = Some(Seq("app3"))),
        Component("app3", dependsOn = Some(Seq("app2")))
      )

      withEvents(event("app1", CPU_LOAD, Alert))

      component("app1").derivedState shouldBe Alert
      component("app2").derivedState shouldBe Alert
      component("app3").derivedState shouldBe Alert

      withEvents(event("app1", CPU_LOAD, Clear))

      component("app1").derivedState shouldBe NoData
      component("app2").derivedState shouldBe NoData
      component("app3").derivedState shouldBe NoData
    }

    // app2 -> app1 <- app3
    "propagate the derived change to all the components depending on the component" in new Context {
      withComponents(
        Component("app1", dependencyOf = Some(Seq("app2", "app3"))),
        Component("app2", dependsOn = Some(Seq("app1"))),
        Component("app3", dependsOn = Some(Seq("app1")))
      )

      withEvents(event("app1", RAM_USAGE, Warning))

      component("app1").derivedState shouldBe Warning
      component("app2").derivedState shouldBe Warning
      component("app3").derivedState shouldBe Warning

      withEvents(event("app1", RAM_USAGE, Clear))

      component("app1").derivedState shouldBe NoData
      component("app2").derivedState shouldBe NoData
      component("app3").derivedState shouldBe NoData
    }

    // app2 -> app1 <- app3
    // app4 ---^ ^---- app5
    "set the derived state to the highest dependent derived" in new Context {
      withComponents(
        Component("app1", dependsOn = Some(Seq("app2", "app3", "app4", "app5"))),
        Component("app2", dependencyOf = Some(Seq("app1"))),
        Component("app3", dependencyOf = Some(Seq("app1"))),
        Component("app4", dependencyOf = Some(Seq("app1"))),
        Component("app5", dependencyOf = Some(Seq("app1"))),
      )

      withEvents(
        event("app2", CPU_LOAD, NoData),
        event("app3", CPU_LOAD, Clear),
        event("app4", CPU_LOAD, Warning),
        event("app5", CPU_LOAD, Alert),
      )

      component("app1").derivedState shouldBe Alert
      component("app2").derivedState shouldBe NoData
      component("app3").derivedState shouldBe NoData
      component("app4").derivedState shouldBe Warning
      component("app5").derivedState shouldBe Alert
    }
  }

  "When the component has a circular dependency" should {

    // app1 <-> app2
    "update its state and the derived state of the related component" in new Context {
      withComponents(
        Component("app1", dependsOn = Some(Seq("app2")), dependencyOf = Some(Seq("app2"))),
        Component("app2", dependsOn = Some(Seq("app1")), dependencyOf = Some(Seq("app1")))
      )

      withEvents(event("app1", CPU_LOAD, Warning))

      component("app1").ownState shouldBe Warning
      component("app1").derivedState shouldBe Warning

      component("app2").ownState shouldBe NoData
      component("app2").derivedState shouldBe Warning
    }

    // app1 <-> app2
    "update the states without propagating the clear state" in new Context {
      withComponents(
        Component("app1", dependsOn = Some(Seq("app2")), dependencyOf = Some(Seq("app2"))),
        Component("app2", dependsOn = Some(Seq("app1")), dependencyOf = Some(Seq("app1")))
      )

      withEvents(
        event("app1", RAM_USAGE, Warning),
        event("app1", RAM_USAGE, Clear)
      )

      component("app1").ownState shouldBe Clear
      component("app1").derivedState shouldBe NoData

      component("app2").ownState shouldBe NoData
      component("app2").derivedState shouldBe NoData
    }

    // app4 -> app3 -> app2 -> app1
    //  ^-----------------------|
    "be able to recover in graph with circular dependency of multiple components" in new Context {
      withComponents(
        Component("app1", dependsOn = Some(Seq("app4")), dependencyOf = Some(Seq("app2"))),
        Component("app2", dependsOn = Some(Seq("app1")), dependencyOf = Some(Seq("app3"))),
        Component("app3", dependsOn = Some(Seq("app2")), dependencyOf = Some(Seq("app4"))),
        Component("app4", dependsOn = Some(Seq("app3")), dependencyOf = Some(Seq("app1")))
      )

      withEvents(event("app1", RAM_USAGE, Warning))

      component("app1").ownState shouldBe Warning
      component("app2").ownState shouldBe NoData
      component("app3").ownState shouldBe NoData
      component("app4").ownState shouldBe NoData

      component("app1").derivedState shouldBe Warning
      component("app2").derivedState shouldBe Warning
      component("app3").derivedState shouldBe Warning
      component("app4").derivedState shouldBe Warning

      withEvents(event("app1", RAM_USAGE, Clear))

      component("app1").ownState shouldBe Clear
      component("app2").ownState shouldBe NoData
      component("app3").ownState shouldBe NoData
      component("app4").ownState shouldBe NoData

      component("app1").derivedState shouldBe NoData
      component("app2").derivedState shouldBe NoData
      component("app3").derivedState shouldBe NoData
      component("app4").derivedState shouldBe NoData
    }

    // app1 <-> app2 <-> app3
    //  ^-----------------^
    "be able to recover in graph where all components depend on each other" in new Context {
      withComponents(
        Component("app1", dependsOn = Some(Seq("app2", "app3")), dependencyOf = Some(Seq("app2", "app3"))),
        Component("app2", dependsOn = Some(Seq("app1", "app3")), dependencyOf = Some(Seq("app1", "app3"))),
        Component("app3", dependsOn = Some(Seq("app1", "app2")), dependencyOf = Some(Seq("app1", "app2")))
      )

      withEvents(event("app1", RAM_USAGE, Alert))

      component("app1").ownState shouldBe Alert
      component("app2").ownState shouldBe NoData
      component("app3").ownState shouldBe NoData

      component("app1").derivedState shouldBe Alert
      component("app2").derivedState shouldBe Alert
      component("app3").derivedState shouldBe Alert

      withEvents(event("app1", RAM_USAGE, Clear))

      component("app1").ownState shouldBe Clear
      component("app2").ownState shouldBe NoData
      component("app3").ownState shouldBe NoData

      component("app1").derivedState shouldBe NoData
      component("app2").derivedState shouldBe NoData
      component("app3").derivedState shouldBe NoData
    }
  }

  "When there are inconsistencies in the initial state" should {

    // app1 -|
    //  ^----|
    "handle self-dependencies" in new Context {
      withComponents(Component("app", dependsOn = Some(Seq("app")), dependencyOf = Some(Seq("app"))))

      withEvents(event("app", CPU_LOAD, Clear))

      component("app").checkStates(CPU_LOAD) shouldBe Clear
      component("app").ownState shouldBe Clear
      component("app").derivedState shouldBe NoData
    }

    "handle non-existing dependencies" in new Context {
      withComponents(Component("app", dependsOn = Some(Seq("unknown1")), dependencyOf = Some(Seq("unknown2"))))

      withEvents(event("app", RAM_USAGE, Warning))

      component("app").checkStates(RAM_USAGE) shouldBe Warning
      component("app").ownState shouldBe Warning
      component("app").derivedState shouldBe Warning
    }

    "handle inconsistent dependencyOf" in new Context {
      withComponents(
        Component("app1", dependencyOf = Some(Seq.empty)), // missing app2 in dependencyOf
        Component("app2", dependsOn = Some(Seq("app1")))
      )

      withEvents(event("app1", CPU_LOAD, Warning))

      component("app1").ownState shouldBe Warning
      component("app2").ownState shouldBe NoData

      component("app1").derivedState shouldBe Warning
      component("app2").derivedState shouldBe NoData
    }

    "handle inconsistent dependsOn" in new Context {
      withComponents(
        Component("app1", dependsOn = Some(Seq.empty)), // missing app2 in dependsOn
        Component("app2", dependencyOf = Some(Seq("app1")))
      )

      withEvents(event("app2", CPU_LOAD, Warning))

      component("app1").ownState shouldBe NoData
      component("app2").ownState shouldBe Warning

      component("app1").derivedState shouldBe NoData
      component("app2").derivedState shouldBe Warning
    }
  }
}
