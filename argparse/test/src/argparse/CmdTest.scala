import utest._

object CmdTest extends TestSuite {
  import argparse.default.ArgumentParser

  val tests = Tests {
    test("basic") {
      val parser = new TestParser
      var extra: Seq[String] = Seq.empty
      parser.command("cmd1", args => extra = args)
      parser.parseResult(Seq("cmd1", "exra1", "--extra2", "extra3")) ==> ArgumentParser.Success
      extra ==> Seq("exra1", "--extra2", "extra3")
    }
    test("missing") {
      val parser = new TestParser
      parser.command("cmd1", _ => ())
      parser.parseResult(Nil)
      parser.missing ==> 1
    }
    test("global") {
      val parser = new TestParser
      val global = parser.requiredParam[String]("--global")
      var res: String = ""
      parser.command("cmd1", _ => res = global.value)
      parser.parseResult(Seq("--global", "hello, world | a | b", "cmd1")) ==> ArgumentParser.Success
      res ==> "hello, world | a | b"
    }
    test("global positional") {
      val parser = new TestParser
      val global = parser.requiredParam[String]("global")
      var res: String = ""
      parser.command("cmd1", _ => res = global.value)
      parser.parseResult(Seq("hello, world | a | b", "cmd1")) ==> ArgumentParser.Success
      res ==> "hello, world | a | b"
    }
  }
}
