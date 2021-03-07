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
    test("parser") {
      def mkparser() = new TestParser(
        predefs = Seq(os.pwd / "cmdr" / "test" / "resources" / "predef1")
      )

      test("extranous") {
        val parser = mkparser()
        parser.parseResult(Seq("foo")) ==> ArgParser.Error
      }

      test("expected") {
        val parser = mkparser()
        val p1 = parser.param[Int]("--some-option", 0)
        val p2 = parser.param[String]("--option2", "")
        val p3 = parser.param[Boolean]("--flag", false, flag = true)
        val p4 = parser.param[String]("--option-with-embedded-setting", "")
        val p5 = parser.requiredParam[String]("pos1")

        parser.parseResult(Seq("foo")) ==> ArgParser.Success

        p1() ==> 1
        p2() ==> "hello, world"
        p3() ==> true
        p4() ==> "hello=world"
        p5() ==> "foo"
      }
    }
  }

}
