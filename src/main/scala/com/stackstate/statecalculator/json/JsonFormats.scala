package com.stackstate.statecalculator.json

import com.stackstate.statecalculator.model._
import spray.json.{DefaultJsonProtocol, JsString, JsValue, JsonFormat, RootJsonFormat, deserializationError}

object JsonFormats {

  import DefaultJsonProtocol._

  implicit object StateFormat extends JsonFormat[State] {
    def write(state: State): JsString = state match {
      case NoData => JsString("no_data")
      case Clear => JsString("clear")
      case Warning => JsString("warning")
      case Alert => JsString("alert")
    }

    def read(json: JsValue): State = json match {
      case JsString("no_data") => NoData
      case JsString("clear") => Clear
      case JsString("warning") => Warning
      case JsString("alert") => Alert
      case JsString(other) => deserializationError(s"Unknown state: $other")
      case _ => deserializationError("String expected")
    }
  }

  implicit val componentFormat: RootJsonFormat[Component] =
    jsonFormat(Component, "id", "own_state", "derived_state", "check_states", "depends_on", "dependency_of")

  implicit val eventFormat: RootJsonFormat[Event] =
    jsonFormat(Event, "timestamp", "component", "check_state", "state")

  implicit val graphFormat: RootJsonFormat[Graph] = jsonFormat1(Graph)
  implicit val componentsFormat: RootJsonFormat[Components] = jsonFormat1(Components)
  implicit val eventsFormat: RootJsonFormat[Events] = jsonFormat1(Events)
}
