package com.stackstate.statecalculator.service

import com.stackstate.statecalculator.model.Component
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.collection.immutable.Seq

object ComponentServiceTest {

  val app1 = Component("app1")
  val app2 = Component("app2")
  val app3 = Component("app3")

  trait Context {
    val componentService = new ComponentService()
  }

}

class ComponentServiceTest extends AnyWordSpec with Matchers {

  import ComponentServiceTest._

  "Should be able to save and retrieve component" in new Context {
    componentService.save(app1)

    componentService.findById("app1") shouldBe Some(app1)
    componentService.findById("pap1") shouldBe None
  }

  "Should be able to retrieve components by ids" in new Context {
    componentService.save(app1)
    componentService.save(app2)
    componentService.save(app3)

    componentService.findByIds(Seq("app1", "app3")) shouldBe Seq(app1, app3)
  }

  "Should be able to retrieve all components" in new Context {
    componentService.save(app1)
    componentService.save(app2)
    componentService.save(app3)

    componentService.findAll() shouldBe Seq(app1, app2, app3)
  }
}
