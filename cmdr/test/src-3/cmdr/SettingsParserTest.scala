package cmdr

import utest._

object SettingsParserTest extends TestSuite {


  val tests = Tests {
    test("basic") {
      case class Endpoint(
        host: String,
        port: Int
      )
      case class Endpoints(
        web: Endpoint,
        monitoring: Endpoint
      )
      val parser = new TestParser
      val settings = parser.settings[Endpoints]("--endpoints")
      parser.parseResult(
        List(
          "--endpoints.web.host=localhost",
          "--endpoints.web.port=80",
          "--endpoints.monitoring.host=localhost",
          "--endpoints.monitoring.port=81",
        )
      ) ==> ArgParser.Success
      settings().web.host ==> "localhost"
      settings().web.port ==> 80
      settings().monitoring.host ==> "localhost"
      settings().monitoring.port ==> 81
    }
  }

}
