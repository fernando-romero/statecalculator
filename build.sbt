lazy val akkaVersion = "2.6.1"

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization    := "com.stackstate",
      scalaVersion    := "2.12.10"
    )),
    name := "statecalculator",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %%  "akka-stream"               % akkaVersion,
      "io.spray"          %%  "spray-json"                % "1.3.4",
      "org.scalatest"     %%  "scalatest"                 % "3.0.8" % Test,
      "com.stephenn"      %%  "scalatest-json-jsonassert" % "0.0.5" % Test
    ),
    assemblyJarName in assembly := "statecalculator.jar",
  )

  Global / cancelable := false