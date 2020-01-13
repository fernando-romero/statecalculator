package com.stackstate.statecalculator

import java.nio.file.Paths

import akka.actor.{ActorSystem, Terminated}
import com.stackstate.statecalculator.model.{Components, Graph}
import com.stackstate.statecalculator.service.{ComponentService, JsonService, StateService}
import spray.json._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class Application(jsonService: JsonService,
                  componentService: ComponentService,
                  stateService: StateService) {

  import com.stackstate.statecalculator.json.JsonFormats._

  implicit val system: ActorSystem = ActorSystem()

  def run(componentsFile: String, eventsFile: String): Future[String] =
    for {
      components <- jsonService.readComponents(Paths.get(componentsFile)).map(_.graph.components)
      events <- jsonService.readEvents(Paths.get(eventsFile)).map(_.events)
    } yield {
      components.foreach(componentService.save)
      events.sortBy(_.timestamp).foreach(stateService.processEvent)
      Components(Graph(componentService.findAll())).toJson.prettyPrint
    }

  def shutdown(): Future[Terminated] = system.terminate()
}
