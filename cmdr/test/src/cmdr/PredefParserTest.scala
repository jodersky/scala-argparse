import utest._
import cmdr.TestParser
import cmdr.ArgParser

object PredefParserTest extends TestSuite {

  val tests = Tests {
    test("empty") {
      cmdr.PredefReader.read("") ==> Nil
    }
    test("blank") {
      cmdr.PredefReader.read(
        s"""|
            |${" "}
            |${"\t"}
            |""".stripMargin
      ) ==> Nil
    }
    test("basic") {
      cmdr.PredefReader.read(
        """|a
           |
           |""".stripMargin
      ) ==> List("--a")
    }
    test("whitespace") {
      def check(space: String) =
        cmdr.PredefReader.read(
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
      cmdr.PredefReader.read(
        """|# a
           |""".stripMargin
      ) ==> Nil
    }
    test("comments") {
      cmdr.PredefReader.read(
        """|# a
           |a
           |b # comment
           |""".stripMargin
      ) ==> List("--a", "--b", "# comment")
    }
  }

}
