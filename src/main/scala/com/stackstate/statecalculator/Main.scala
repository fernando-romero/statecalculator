package com.stackstate.statecalculator

import com.stackstate.statecalculator.service._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

object Main {

  def main(args: Array[String]): Unit = {
    if (args.length < 2) {
      System.err.println("Two arguments expected")
      return
    }

    val componentsFile = args(0)
    val eventsFile = args(1)

    val componentsService = new ComponentService()
    val jsonService = new JsonService()
    val stateService = new StateService(componentsService)
    val application = new Application(jsonService, componentsService, stateService)

    application
      .run(componentsFile, eventsFile)
      // .map(System.out.println)
      .andThen {
        case Success(graph) => System.out.println(graph)
        case Failure(e) => System.err.println(e.getMessage)
      }
      .foreach(_ => application.shutdown())
  }
}