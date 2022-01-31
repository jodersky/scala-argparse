package argparse

import utest._

object BashCompletionTest extends TestSuite {

  class CompletionParser() {
    val data = new java.io.ByteArrayOutputStream
    val parser = ArgumentParser(stdout = new java.io.PrintStream(data))
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
      parser.parser.parseResult(Seq(), line("cmd")) ==> ArgumentParser.EarlyExit
      parser.completions.isEmpty
    }
    test("named single") {
      val parser = new CompletionParser()
      parser.parser.requiredParam[String]("--a")
      parser.parser.parseResult(Seq(), line("cmd -")) ==> ArgumentParser.EarlyExit
      parser.completions ==> List("--help ", "--bash-completion ", "--a ")
    }
    test("named multiple") {
      val parser = new CompletionParser()
      parser.parser.requiredParam[String]("--a")
      parser.parser.requiredParam[String]("--b")
      parser.parser.parseResult(Seq(), line("cmd -")) ==> ArgumentParser.EarlyExit
      parser.completions ==> List("--help ", "--bash-completion ", "--a ", "--b ")
    }
    test("named multiple partial match 1") {
      val parser = new CompletionParser()
      parser.parser.requiredParam[String]("--option")
      parser.parser.requiredParam[String]("--opt")
      parser.parser.requiredParam[String]("--optimize")
      parser.parser.parseResult(Seq(), line("cmd --opt")) ==> ArgumentParser.EarlyExit
      parser.completions ==> List("--option ", "--opt ", "--optimize ")
    }
    test("named multiple partial match 2") {
      val parser = new CompletionParser()
      parser.parser.requiredParam[String]("--option")
      parser.parser.requiredParam[String]("--opt")
      parser.parser.requiredParam[String]("--optimize")
      parser.parser.parseResult(Seq(), line("cmd --opti")) ==> ArgumentParser.EarlyExit
      parser.completions ==> List("--option ", "--optimize ")
    }
    test("named multiple partial match 3") {
      val parser = new CompletionParser()
      parser.parser.requiredParam[String]("--option")
      parser.parser.requiredParam[String]("--opt")
      parser.parser.requiredParam[String]("--optimize")
      parser.parser.parseResult(Seq(), line("cmd --optim")) ==> ArgumentParser.EarlyExit
      parser.completions ==> List("--optimize ")
    }
    test("named multiple full match") {
      val parser = new CompletionParser()
      parser.parser.requiredParam[String]("--option")
      parser.parser.requiredParam[String]("--opt")
      parser.parser.requiredParam[String]("--optimize")
      parser.parser.parseResult(Seq(), line("cmd --optimize")) ==> ArgumentParser.EarlyExit
      parser.completions ==> List("--optimize ")
    }
    test("arg completions") {
      val parser = new CompletionParser()
      parser.parser.requiredParam[String]("--option")
      parser.parser.requiredParam[String]("--opt")
      parser.parser.requiredParam[String](
        "--optimize",
        completer =
          prefix => Seq("L1 ", "L2 ", "L11 ").filter(_.startsWith(prefix))
      )
      parser.parser.parseResult(Seq(), line("cmd --optimize ")) ==> ArgumentParser.EarlyExit
      parser.completions ==> List("L1 ", "L2 ", "L11 ")
    }
    test("arg completions partial") {
      val parser = new CompletionParser()
      parser.parser.requiredParam[String]("--option")
      parser.parser.requiredParam[String]("--opt")
      parser.parser.requiredParam[String](
        "--optimize",
        completer =
          prefix => Seq("L1 ", "L2 ", "L11 ").filter(_.startsWith(prefix))
      )
      parser.parser.parseResult(Seq(), line("cmd --optimize L1")) ==> ArgumentParser.EarlyExit
      parser.completions ==> List("L1 ", "L11 ")
    }
    test("arg completions partial embedded") {
      val parser = new CompletionParser()
      parser.parser.requiredParam[String]("--option")
      parser.parser.requiredParam[String]("--opt")
      parser.parser.requiredParam[String](
        "--optimize",
        completer =
          prefix => Seq("L1 ", "L2 ", "L11 ").filter(_.startsWith(prefix))
      )
      parser.parser.parseResult(Seq(), line("cmd --optimize=L1")) ==> ArgumentParser.EarlyExit
      parser.completions ==> List("L1 ", "L11 ")
    }
  }

}
