import utest._
import argparse.TestParser
import argparse.ArgParser

object PredefParserTest extends TestSuite {

  val tests = Tests {
    test("empty") {
      argparse.PredefReader.read("") ==> Nil
    }
    test("blank") {
      argparse.PredefReader.read(
        s"""|
            |${" "}
            |${"\t"}
            |""".stripMargin
      ) ==> Nil
    }
    test("basic") {
      argparse.PredefReader.read(
        """|a
           |
           |""".stripMargin
      ) ==> List("--a")
    }
    test("whitespace") {
      def check(space: String) =
        argparse.PredefReader.read(
          s"""|a$space
              |a${space}b
              |a${space}b${space}
              |a${space} b${space}
              |${space}a${space}
              |${space}${space}a${space}${space}${space}b""".stripMargin
        ) ==> List("--a", "--a", "b", "--a", "b", "--a", "b", "--a", "--a", "b")

      test("space") - check(" ")
      test("tab") - check("\t")
    }
    test("comments simple") {
      argparse.PredefReader.read(
        """|# a
           |""".stripMargin
      ) ==> Nil
    }
    test("comments") {
      argparse.PredefReader.read(
        """|# a
           |a
           |b # comment
           |""".stripMargin
      ) ==> List("--a", "--b", "# comment")
    }
  }

}
