package com.stackstate.statecalculator.json

import com.stackstate.statecalculator.model._
import com.stephenn.scalatest.jsonassert.JsonMatchers
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import spray.json._

class JsonFormatsTest extends AnyWordSpec with Matchers with JsonMatchers {

  import JsonFormats._

  "StateFormat should provide serialization support for State" in {
    val noData: State = NoData
    val clear: State = Clear
    val warning: State = Warning
    val alert: State = Alert

    noData.toJson shouldBe JsString("no_data")
    clear.toJson shouldBe JsString("clear")
    warning.toJson shouldBe JsString("warning")
    alert.toJson shouldBe JsString("alert")

    JsString("no_data").convertTo[State] shouldBe NoData
    JsString("clear").convertTo[State] shouldBe Clear
    JsString("warning").convertTo[State] shouldBe Warning
    JsString("alert").convertTo[State] shouldBe Alert

    def badState = JsString("unknown").convertTo[State]

    the[DeserializationException] thrownBy badState should have message "Unknown state: unknown"

    def badType = JsNumber(1).convertTo[State]

    the[DeserializationException] thrownBy badType should have message "String expected"
  }

  "componentFormat should provide serialization support for Component" in {
    val component = Component("app")
    val json =
      """{
        |   "id":"app",
        |   "own_state":"no_data",
        |   "derived_state":"no_data",
        |   "check_states":{
        |
        |   }
        |}""".stripMargin

    component.toJson.prettyPrint should matchJson(json)
    json.parseJson.convertTo[Component] shouldBe component
  }

  "eventFormat should provide serialization support for Event" in {
    val event = Event("1", "app", "CPU", NoData)
    val json =
      """{
        |   "timestamp":"1",
        |   "component":"app",
        |   "check_state":"CPU",
        |   "state":"no_data"
        |}""".stripMargin

    event.toJson.prettyPrint should matchJson(json)
    json.parseJson.convertTo[Event] shouldBe event
  }
}
