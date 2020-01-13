package com.stackstate.statecalculator.model

import scala.collection.immutable.Seq

object State {
  implicit def stateOrdering: Ordering[State] = Ordering.by(_.value)
}

sealed trait State {
  private[model] val value: Int

  def <(than: State): Boolean = this.value < than.value

  def >(than: State): Boolean = this.value > than.value
}

case object NoData extends State {
  override private[model] val value = 0
}

case object Clear extends State {
  override private[model] val value = 1
}

case object Warning extends State {
  override private[model] val value = 2
}

case object Alert extends State {
  override private[model] val value = 3
}

case class Components(graph: Graph)

case class Graph(components: Seq[Component])

case class Component(id: String,
                     ownState: State = NoData,
                     derivedState: State = NoData,
                     checkStates: Map[String, State] = Map.empty,
                     dependsOn: Option[Seq[String]] = None,
                     dependencyOf: Option[Seq[String]] = None)

case class Events(events: Seq[Event])

case class Event(timestamp: String, component: String, checkState: String, state: State)