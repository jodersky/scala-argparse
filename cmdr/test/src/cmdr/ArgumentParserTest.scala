package cmdr

import utest._

object ArgumentParserTest extends TestSuite {

  class TestParser extends ArgumentParser("", "", "") {
    var missing = 0
    override def reportMissing(name: String): Unit = missing += 1
    var unknown = 0
    override def reportUnknown(name: String): Unit = unknown += 1
    var parseErrors = 0
    override def reportParseError(
        name: String,
        message: String
    ): Unit = parseErrors += 1
    override def check(): Unit = ()
  }

  val tests = Tests {
    test("empty") {
      val parser = new TestParser
      parser.parse(Nil)

      parser.missing ==> 0
      parser.unknown ==> 0
    }
    test("positional") {
      val parser = new TestParser
      parser.requiredParam[String]("p1")
      parser.requiredParam[String]("p2")
      parser.parse(Nil)

      parser.missing ==> 2
      parser.unknown ==> 0
    }
    test("positional with defaults") {
      val parser = new TestParser
      parser.param[String]("p1", "a")
      parser.param[String]("p2", "b")
      parser.parse(Nil)

      parser.missing ==> 0
      parser.unknown ==> 0
    }
    test("positional partial") {
      val parser = new TestParser
      parser.requiredParam[String]("p1")
      parser.requiredParam[String]("p2")
      parser.parse("foo" :: Nil)

      parser.missing ==> 1
      parser.unknown ==> 0
    }
    test("named args") {
      val parser = new TestParser
      parser.requiredParam[String]("--p1")
      parser.requiredParam[String]("--p2")
      parser.parse("--p1=a" :: "--p2=b" :: Nil)

      parser.missing ==> 0
      parser.unknown ==> 0
    }
    test("named args missing") {
      val parser = new TestParser
      parser.requiredParam[String]("--p1")
      parser.requiredParam[String]("--p2")
      parser.parse(Nil)

      parser.missing ==> 2
      parser.unknown ==> 0
    }
    test("named args missing default") {
      val parser = new TestParser
      parser.param[String]("--p1", "a")
      parser.requiredParam[String]("--p2")
      parser.parse(Nil)

      parser.missing ==> 1
      parser.unknown ==> 0
    }
    test("named args too many") {
      val parser = new TestParser
      parser.requiredParam[String]("--p1")
      parser.requiredParam[String]("--p2")
      parser.parse("--p2=a" :: "--p3=c" :: Nil)

      parser.missing ==> 1
      parser.unknown ==> 1
    }
    test("named and positional") {
      val parser = new TestParser
      val n1 = parser.requiredParam[String]("--n1")
      val n2 = parser.requiredParam[String]("--n2")
      val p1 = parser.requiredParam[String]("p1")
      val p2 = parser.requiredParam[String]("p2")
      val n3 = parser.requiredParam[String]("--n3")
      parser.parse("--n2=a" :: "b" :: "--n3=c" :: "d" :: "--n1=e" :: Nil)

      n1.get ==> "e"
      n2.get ==> "a"
      n3.get ==> "c"
      p1.get ==> "b"
      p2.get ==> "d"
      parser.missing ==> 0
      parser.unknown ==> 0
    }
    test("escaping") {
      val parser = new TestParser
      val n1 = parser.requiredParam[String]("--n1")
      val p1 = parser.requiredParam[String]("p1")
      val p2 = parser.requiredParam[String]("p2")
      parser.parse("a" :: "--" :: "--n3=c" :: Nil)

      p1.get ==> "a"
      p2.get ==> "--n3=c"
      parser.missing ==> 1
      parser.unknown ==> 0
    }
    test("typed int") {
      val parser = new TestParser
      val n1 = parser.param("--n1", 2)
      parser.parse("--n1=2" :: Nil)

      parser.missing ==> 0
      parser.unknown ==> 0
      parser.parseErrors ==> 0
      n1.get ==> 2
    }
    test("typed error") {
      val parser = new TestParser
      val n1 = parser.param[Int]("--n1", 2)
      parser.parse("--n1=2.2" :: Nil)

      parser.missing ==> 0
      parser.unknown ==> 0
      parser.parseErrors ==> 1
    }
    test("typed list") {
      val parser = new TestParser
      val n1 = parser.requiredParam[List[Int]]("--n1")
      parser.parse("--n1=2,3" :: Nil)

      parser.missing ==> 0
      parser.unknown ==> 0
      parser.parseErrors ==> 0
      n1.get ==> List(2, 3)
    }
    test("repeated named") {
      val parser = new TestParser
      val n1 = parser.repeatedParam[String]("--n1")
      parser.parse("--n1=a" :: "--n1=b" :: "--n1=c" :: Nil)

      parser.missing ==> 0
      parser.unknown ==> 0
      parser.parseErrors ==> 0
      n1.get ==> List("a", "b", "c")
    }
    test("repeated positional") {
      val parser = new TestParser
      val n1 = parser.repeatedParam[String]("--n1")
      val p1 = parser.requiredParam[String]("p1")
      val r1 = parser.repeatedParam[String]("r1")
      parser.parse("a" :: "b" :: "--n1=c" :: "d" :: "e" :: Nil)

      parser.missing ==> 0
      parser.unknown ==> 0
      parser.parseErrors ==> 0
      n1.get ==> List("c")
      p1.get ==> "a"
      r1.get ==> List("b", "d", "e")
    }
    test("repeated mix") {
      val parser = new TestParser
      val n1 = parser.repeatedParam[String]("--n1")
      val n2 = parser.param[String]("--n2", "none") // not a repeated param, last value overrides
      val p1 = parser.requiredParam[String]("p1")
      val r1 = parser.repeatedParam[String]("r1")
      parser.parse(
        List(
          "--n1=a",
          "b",
          "c",
          "--n2=d",
          "--n1=e",
          "--n2=f",
          "g",
          "h",
          "--",
          "--n2=i"
        )
      )

      parser.missing ==> 0
      parser.unknown ==> 0
      parser.parseErrors ==> 0
      n1.get ==> List("a", "e")
      n2.get ==> "f"
      p1.get ==> "b"
      r1.get ==> List("c", "g", "h", "--n2=i")
    }
    test("missing argument's argument") {
      val parser = new TestParser
      val n1 = parser.requiredParam[String]("--n1")
      parser.parse("--n1" :: Nil)

      parser.missing ==> 1
      parser.unknown ==> 0
      parser.parseErrors ==> 1
    }
    test("flag") {
      val parser = new TestParser
      val n1 = parser.requiredParam[String]("--n1", flag=true)
      parser.parse("--n1" :: Nil)

      parser.missing ==> 0
      parser.unknown ==> 0
      parser.parseErrors ==> 0
      n1.get ==> "true"
    }
    test("flag override with embedded") {
      val parser = new TestParser
      val n1 = parser.requiredParam[String]("--n1", flag=true)
      parser.parse("--n1=yesss!" :: Nil)

      parser.missing ==> 0
      parser.unknown ==> 0
      parser.parseErrors ==> 0
      n1.get ==> "yesss!"
    }
    test("flag absent without default value") {
      val parser = new TestParser
      val n1 = parser.requiredParam[String]("--n1", flag=true) // a flag without default value doesn't make much sense but is possible
      parser.parse(Nil)

      parser.missing ==> 1
      parser.unknown ==> 0
      parser.parseErrors ==> 0
    }
    test("flag absent with default value") {
      val parser = new TestParser
      val n1 = parser.param[String]("--n1", "false", flag=true)
      parser.parse(Nil)

      parser.missing ==> 0
      parser.unknown ==> 0
      parser.parseErrors ==> 0
      n1.get ==> "false"
    }
    test("named, not embedded") {
      val parser = new TestParser
      val n1 = parser.requiredParam[String]("--n1")
      val n2 = parser.requiredParam[String]("--n2")
      val p1 = parser.requiredParam[String]("p1")
      parser.parse("--n1" :: "a" :: "--n1" :: "b" :: "c" :: "--n2" :: "d" :: Nil)

      parser.missing ==> 0
      parser.unknown ==> 0
      parser.parseErrors ==> 0
      n1.get ==> "b"
      n2.get ==> "d"
      p1.get ==> "c"
    }
  }
}
