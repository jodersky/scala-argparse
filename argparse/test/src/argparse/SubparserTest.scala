import utest._

object SubparserTest extends TestSuite {
  import argparse.default.ArgumentParser
  import argparse.ParseResult

  val tests = Tests {
    test("basic") {
      val parser = ArgumentParser()
      val nested = parser.subparser("cmd1")
      val extra1 = nested.requiredParam[String]("extra1")
      val extra2 = nested.requiredParam[String]("--extra2")

      parser.parseResult(Seq("cmd1", "extra1", "--extra2", "extra3")) ==> ParseResult.Success
      extra1.value ==> "extra1"
      extra2.value ==> "extra3"
    }
    test("missing") {
      val parser = new TestParser
      val nested = parser.subparser("cmd1")
      parser.parseResult(Nil)
      parser.missing ==> 1
    }
    test("global") {
      val parser = ArgumentParser()
      val global = parser.requiredParam[String]("--global")
      parser.subparser("cmd1")
      parser.parseResult(Seq("--global", "hello, world | a | b", "cmd1")) ==> ParseResult.Success
      global.value ==> "hello, world | a | b"
    }
    test("global positional") {
      val parser = ArgumentParser()
      val global = parser.requiredParam[String]("global")
      parser.subparser("cmd1")
      parser.parseResult(Seq("hello, world | a | b", "cmd1")) ==> ParseResult.Success
      global.value ==> "hello, world | a | b"
    }
  }
}
