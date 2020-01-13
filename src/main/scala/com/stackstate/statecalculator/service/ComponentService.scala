package com.stackstate.statecalculator.service

import com.stackstate.statecalculator.model.Component

import scala.collection.immutable.Seq

class ComponentService {
  private var components: Seq[Component] = Seq.empty

  def save(component: Component): Component = {
    components = components.filterNot(_.id == component.id) :+ component
    component
  }

  def findById(id: String): Option[Component] = components.find(_.id == id)

  def findByIds(ids: Seq[String]): Seq[Component] = components.filter(c => ids.contains(c.id))

  def findAll(): Seq[Component] = components
}
