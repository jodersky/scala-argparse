package argparse

import utest._

object MutableSettingsParserTest extends TestSuite {

  class Settings {
    var opt1: String = "hello"
    var opt2: Int = 0
    var flag: Boolean = false

    /** help: help message
      * env: ENV_VAR
      */
    var extraOpt = ""

    object http {
      var port = 0
      var host = ""

      object nested {
        var a = 1
        var b = 2
      }
    }
  }

  val tests = Tests {
    test("empty") {
      val parser = new TestParser
      val settings = parser.mutableSettings(Settings())
      parser.parseResult(Nil) ==> ArgumentParser.Success
    }
    test("unknown") {
      val parser = new TestParser
      val settings = parser.mutableSettings(Settings())
      parser.parseResult(Seq("--unknown")) ==> ArgumentParser.Error
      parser.unknown ==> 1
    }
    test("valid") {
      val parser = new TestParser
      val settings = parser.mutableSettings(Settings())
      // format: off
      parser.parseResult(List(
        "--opt1", "world",
        "--opt2=42",
        "--flag",
        "--extra-opt=ok",
        "--http.port", "8080",
        "--http.nested.a", "42"
      )) ==> ArgumentParser.Success
      // format: on

      settings.opt1 ==> "world"
      settings.opt2 ==> 42
      settings.flag ==> true
      settings.extraOpt ==> "ok"
      settings.http.port ==> 8080
      settings.http.host ==> ""
      settings.http.nested.a ==> 42
      settings.http.nested.b ==> 2
    }
    test("mixed") {
      val parser = new TestParser
      val settings = parser.mutableSettings(Settings())
      val param1 = parser.param[String]("--opt3", "a")
      val positional = parser.requiredParam[String]("pos")

      parser.parseResult(Seq("--http.port=8080", "pos", "--opt3", "b")) ==> ArgumentParser.Success

      settings.http.port ==> 8080
      param1() ==> "b"
      positional() ==> "pos"
    }
    test("inherited") {
      trait Base {
        var opt1 = ""
      }
      class Config extends Base {
        var opt2 = ""
      }
      val parser = new TestParser
      val settings = parser.mutableSettings(Config())
      parser.parseResult(Seq("--opt1", "a", "--opt2", "b")) ==> ArgumentParser.Success

      settings.opt1 ==> "a"
      settings.opt2 ==> "b"
    }
  }

}
