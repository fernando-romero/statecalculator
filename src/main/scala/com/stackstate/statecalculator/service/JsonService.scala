package com.stackstate.statecalculator.service

import java.nio.file.Path

import akka.actor.ActorSystem
import akka.stream.scaladsl.FileIO
import akka.util.ByteString
import com.stackstate.statecalculator.model.{Components, Events}
import spray.json._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class JsonService {

  def readComponents(file: Path)(implicit reader: JsonReader[Components], system: ActorSystem): Future[Components] =
    read(file)

  def readEvents(file: Path)(implicit reader: JsonReader[Events], system: ActorSystem): Future[Events] =
    read(file)

  private def read[A](file: Path)(implicit reader: JsonReader[A], system: ActorSystem): Future[A] =
    FileIO
      .fromPath(file)
      .runFold(ByteString.empty)(_ ++ _)
      .map(_.utf8String)
      .map(_.parseJson)
      .map(_.convertTo[A])
}
