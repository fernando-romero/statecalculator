package com.stackstate.statecalculator

import java.io.{ByteArrayOutputStream, PrintStream}

import com.stephenn.scalatest.jsonassert.JsonMatchers
import org.scalatest.concurrent.Eventually
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import tags.Integration

object MainTest {

  val componentsFile = "src/test/resources/sample-initial.json"
  val eventsFile = "src/test/resources/sample-events.json"

  trait Context {
    val bo = new ByteArrayOutputStream
  }

}

@Integration
class MainTest extends AnyWordSpec with Matchers with JsonMatchers with Eventually {

  import MainTest._

  "Should read from the components and events file and output a new state" in new Context {
    System.setOut(new PrintStream(bo))
    Main.main(Array(componentsFile, eventsFile))

    eventually {
      new String(bo.toByteArray) should matchJson(
        """{
          |  "graph": {
          |    "components": [{
          |      "check_states": {
          |        "CPU load": "warning",
          |        "RAM usage": "no_data"
          |      },
          |      "id": "db",
          |      "own_state": "warning",
          |      "derived_state": "warning",
          |      "dependency_of": ["app"]
          |    }, {
          |      "check_states": {
          |        "CPU load": "clear",
          |        "RAM usage": "no_data"
          |      },
          |      "depends_on": ["db"],
          |      "id": "app",
          |      "own_state": "clear",
          |      "derived_state": "warning"
          |    }]
          |  }
          |}""".stripMargin)
    }
  }

  "Should report an error if amount of arguments is less than 2" in new Context {
    System.setErr(new PrintStream(bo))
    Main.main(Array(componentsFile))

    new String(bo.toByteArray) should include("Two arguments expected")
  }

  "Should report errors to standard error" in new Context {
    System.setErr(new PrintStream(bo))
    Main.main(Array(eventsFile, componentsFile)) // inverted

    eventually {
      new String(bo.toByteArray) should include("Object is missing required member 'graph'")
    }
  }
}
