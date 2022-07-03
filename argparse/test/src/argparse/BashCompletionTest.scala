import utest._

object BashCompletionTest extends TestSuite {
  import argparse.default.ArgumentParser

  class CompletionParser(env: Map[String, String]) {
    val data = new java.io.ByteArrayOutputStream
    val parser = ArgumentParser(stdout = new java.io.PrintStream(data), env = env)
    def completions = data.toString("utf-8").split("\n").toList
  }

  // generate environment vars that would be given by bash completion
  def line(partialLine: String) =  Map(
    "COMP_LINE" -> partialLine,
    "COMP_POINT" -> partialLine.length.toString()
  )

  val tests = Tests {
    test("basic") {
      val parser = new CompletionParser(line("cmd"))
      parser.parser.parseResult(Seq()) ==> ArgumentParser.EarlyExit
      parser.completions ==> List("")
    }
    test("named single") {
      val parser = new CompletionParser(line("cmd -"))
      parser.parser.requiredParam[String]("--a")
      parser.parser.parseResult(Seq()) ==> ArgumentParser.EarlyExit
      parser.completions ==> List("--a ")
    }
    test("named multiple") {
      val parser = new CompletionParser(line("cmd -"))
      parser.parser.requiredParam[String]("--a")
      parser.parser.requiredParam[String]("--b")
      parser.parser.parseResult(Seq()) ==> ArgumentParser.EarlyExit
      parser.completions ==> List("--a ", "--b ")
    }
    test("named multiple partial match 1") {
      val parser = new CompletionParser(line("cmd --opt"))
      parser.parser.requiredParam[String]("--option")
      parser.parser.requiredParam[String]("--opt")
      parser.parser.requiredParam[String]("--optimize")
      parser.parser.parseResult(Seq()) ==> ArgumentParser.EarlyExit
      parser.completions ==> List("--option ", "--opt ", "--optimize ")
    }
    test("named multiple partial match 2") {
      val parser = new CompletionParser(line("cmd --opti"))
      parser.parser.requiredParam[String]("--option")
      parser.parser.requiredParam[String]("--opt")
      parser.parser.requiredParam[String]("--optimize")
      parser.parser.parseResult(Seq()) ==> ArgumentParser.EarlyExit
      parser.completions ==> List("--option ", "--optimize ")
    }
    test("named multiple partial match 3") {
      val parser = new CompletionParser(line("cmd --optim"))
      parser.parser.requiredParam[String]("--option")
      parser.parser.requiredParam[String]("--opt")
      parser.parser.requiredParam[String]("--optimize")
      parser.parser.parseResult(Seq()) ==> ArgumentParser.EarlyExit
      parser.completions ==> List("--optimize ")
    }
    test("named multiple full match") {
      val parser = new CompletionParser(line("cmd --optimize"))
      parser.parser.requiredParam[String]("--option")
      parser.parser.requiredParam[String]("--opt")
      parser.parser.requiredParam[String]("--optimize")
      parser.parser.parseResult(Seq()) ==> ArgumentParser.EarlyExit
      parser.completions ==> List("--optimize ")
    }
    test("arg completions") {
      val parser = new CompletionParser(line("cmd --optimize "))
      parser.parser.requiredParam[String]("--option")
      parser.parser.requiredParam[String]("--opt")
      parser.parser.requiredParam[String](
        "--optimize",
        interactiveCompleter =
          prefix => Seq("L1 ", "L2 ", "L11 ").filter(_.startsWith(prefix))
      )
      parser.parser.parseResult(Seq()) ==> ArgumentParser.EarlyExit
      parser.completions ==> List("L1 ", "L2 ", "L11 ")
    }
    test("arg completions partial") {
      val parser = new CompletionParser(line("cmd --optimize L1"))
      parser.parser.requiredParam[String]("--option")
      parser.parser.requiredParam[String]("--opt")
      parser.parser.requiredParam[String](
        "--optimize",
        interactiveCompleter =
          prefix => Seq("L1 ", "L2 ", "L11 ").filter(_.startsWith(prefix))
      )
      parser.parser.parseResult(Seq()) ==> ArgumentParser.EarlyExit
      parser.completions ==> List("L1 ", "L11 ")
    }
    test("arg completions partial embedded") {
      val parser = new CompletionParser(line("cmd --optimize=L1"))
      parser.parser.requiredParam[String]("--option")
      parser.parser.requiredParam[String]("--opt")
      parser.parser.requiredParam[String](
        "--optimize",
        interactiveCompleter =
          prefix => Seq("L1 ", "L2 ", "L11 ").filter(_.startsWith(prefix))
      )
      parser.parser.parseResult(Seq()) ==> ArgumentParser.EarlyExit
      parser.completions ==> List("L1 ", "L11 ")
    }
  }

}
