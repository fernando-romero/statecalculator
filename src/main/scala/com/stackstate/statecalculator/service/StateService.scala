package com.stackstate.statecalculator.service

import com.stackstate.statecalculator.model._

class StateService(componentService: ComponentService) {

  import State.stateOrdering

  def processEvent(event: Event): Unit = {
    updateCheckState(event)
    propagate(Nil, updateOwnState)(event.component)
    propagate(Nil, updateDerivedState)(event.component)
  }

  private def updateCheckState(event: Event): Unit =
    componentService.findById(event.component).foreach { component =>
      val checkStates = component.checkStates + (event.checkState -> event.state)
      val updatedComponent = component.copy(checkStates = checkStates)
      componentService.save(updatedComponent)
    }

  private def updateOwnState(id: String): Unit =
    componentService.findById(id).foreach { component =>
      val ownState = if (component.checkStates.isEmpty) NoData else component.checkStates.values.max
      val derivedState = if (ownState < Warning) NoData else ownState
      val updatedComponent = component.copy(ownState = ownState, derivedState = derivedState)
      componentService.save(updatedComponent)
    }

  private def updateDerivedState(id: String): Unit =
    componentService.findById(id).foreach { component =>
      val derivedState = (component.ownState, dependentDerivedState(component)) match {
        case (own, dependent) if own < Warning && dependent < Warning => NoData
        case (own, dependent) => if (own > dependent) own else dependent
      }
      val updatedComponent = component.copy(derivedState = derivedState)
      componentService.save(updatedComponent)
    }

  private def propagate(processedIds: Seq[String], update: String => Unit)(id: String): Unit =
    if (!processedIds.contains(id)) {
      componentService.findById(id).foreach { component =>
        update(component.id)
        component.dependencyOf.foreach { dependencyOf =>
          dependencyOf.foreach(propagate(processedIds :+ id, update))
        }
      }
    }

  private def dependentDerivedState(component: Component): State =
    component.dependsOn match {
      case Some(dependsOn) if dependsOn.nonEmpty =>
        componentService
          .findByIds(dependsOn)
          .map(_.derivedState)
          .sorted
          .lastOption
          .getOrElse(NoData)
      case _ => NoData
    }
}
