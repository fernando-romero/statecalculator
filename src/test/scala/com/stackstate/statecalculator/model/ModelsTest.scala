package com.stackstate.statecalculator.model

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ModelsTest extends AnyWordSpec with Matchers {

  import State.stateOrdering

  "State values should have the order: NoData < Clear < Warning < NoData" in {
    Seq[State](Warning, NoData, Alert, Clear).sorted shouldBe Seq(NoData, Clear, Warning, Alert)
  }

  "State should provide > and < operators" in {
    Clear > NoData shouldBe true
    Warning > NoData shouldBe true
    Warning > Clear shouldBe true
    Alert > NoData shouldBe true
    Alert > Clear shouldBe true
    Alert > Warning shouldBe true

    NoData < Clear shouldBe true
    NoData < Warning shouldBe true
    NoData < Alert shouldBe true
    Clear < Warning shouldBe true
    Clear < Alert shouldBe true
    Warning < Alert shouldBe true
  }
}
