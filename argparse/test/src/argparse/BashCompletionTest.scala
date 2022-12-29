import utest._

object BashCompletionTest extends TestSuite {
  import argparse.default.ArgumentParser
  import argparse.ParseResult

  class CompletionParser() {
    val data = new java.io.ByteArrayOutputStream
    val parser = ArgumentParser()
    def parseResult(args: Seq[String], env: Map[String, String]) =
      parser.parseResult(args, env, stdout = new java.io.PrintStream(data))
    def completions = data.toString("utf-8").split("\n").toList
  }

  // generate environment vars that would be given by bash completion
  def line(partialLine: String) =  Map(
    "COMP_LINE" -> partialLine,
    "COMP_POINT" -> partialLine.length.toString()
  )

  val tests = Tests {
    test("basic") {
      val parser = new CompletionParser()
      parser.parseResult(Seq(), line("cmd")) ==> ParseResult.EarlyExit
      parser.completions ==> List("")
    }
    test("named single") {
      val parser = new CompletionParser()
      parser.parser.requiredParam[String]("--a")
      parser.parseResult(Seq(), line("cmd -")) ==> ParseResult.EarlyExit
      parser.completions ==> List( "--a ", "--bash-completion ", "--help ")
    }
    test("named multiple") {
      val parser = new CompletionParser()
      parser.parser.requiredParam[String]("--a")
      parser.parser.requiredParam[String]("--b")
      parser.parseResult(Seq(), line("cmd -")) ==> ParseResult.EarlyExit
      parser.completions ==> List("--a ", "--b ", "--bash-completion ", "--help ")
    }
    test("named multiple partial match 1") {
      val parser = new CompletionParser()
      parser.parser.requiredParam[String]("--option")
      parser.parser.requiredParam[String]("--opt")
      parser.parser.requiredParam[String]("--optimize")
      parser.parseResult(Seq(), line("cmd --opt")) ==> ParseResult.EarlyExit
      parser.completions ==> List("--opt ", "--optimize ", "--option ")
    }
    test("named multiple partial match 2") {
      val parser = new CompletionParser()
      parser.parser.requiredParam[String]("--option")
      parser.parser.requiredParam[String]("--opt")
      parser.parser.requiredParam[String]("--optimize")
      parser.parseResult(Seq(), line("cmd --opti")) ==> ParseResult.EarlyExit
      parser.completions ==> List("--optimize ", "--option ")
    }
    test("named multiple partial match 3") {
      val parser = new CompletionParser()
      parser.parser.requiredParam[String]("--option")
      parser.parser.requiredParam[String]("--opt")
      parser.parser.requiredParam[String]("--optimize")
      parser.parseResult(Seq(), line("cmd --optim")) ==> ParseResult.EarlyExit
      parser.completions ==> List("--optimize ")
    }
    test("named multiple full match") {
      val parser = new CompletionParser()
      parser.parser.requiredParam[String]("--option")
      parser.parser.requiredParam[String]("--opt")
      parser.parser.requiredParam[String]("--optimize")
      parser.parseResult(Seq(), line("cmd --optimize")) ==> ParseResult.EarlyExit
      parser.completions ==> List("--optimize ")
    }
    test("arg completions") {
      val parser = new CompletionParser()
      parser.parser.requiredParam[String]("--option")
      parser.parser.requiredParam[String]("--opt")
      parser.parser.requiredParam[String](
        "--optimize",
        interactiveCompleter =
          prefix => Seq("L1 ", "L2 ", "L11 ").filter(_.startsWith(prefix))
      )
      parser.parseResult(Seq(), line("cmd --optimize ")) ==> ParseResult.EarlyExit
      parser.completions ==> List("L1 ", "L2 ", "L11 ")
    }
    test("arg completions partial") {
      val parser = new CompletionParser()
      parser.parser.requiredParam[String]("--option")
      parser.parser.requiredParam[String]("--opt")
      parser.parser.requiredParam[String](
        "--optimize",
        interactiveCompleter =
          prefix => Seq("L1 ", "L2 ", "L11 ").filter(_.startsWith(prefix))
      )
      parser.parseResult(Seq(), line("cmd --optimize L1")) ==> ParseResult.EarlyExit
      parser.completions ==> List("L1 ", "L11 ")
    }
    test("arg completions partial embedded") {
      val parser = new CompletionParser()
      parser.parser.requiredParam[String]("--option")
      parser.parser.requiredParam[String]("--opt")
      parser.parser.requiredParam[String](
        "--optimize",
        interactiveCompleter =
          prefix => Seq("L1 ", "L2 ", "L11 ").filter(_.startsWith(prefix))
      )
      parser.parseResult(Seq(), line("cmd --optimize=L1")) ==> ParseResult.EarlyExit
      parser.completions ==> List("L1 ", "L11 ")
    }
  }

}
