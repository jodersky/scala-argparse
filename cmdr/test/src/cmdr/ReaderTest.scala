package cmdr

import utest._

object ReaderTest extends TestSuite {

  val tests = Tests {
    test("int") {
      val parser = new TestParser
      parser.requiredParam[Int]("p1")
      parser.parseResult(Seq("1"))
      parser.parseErrors ==> 0
    }
    test("int is not float") {
      val parser = new TestParser
      parser.requiredParam[Int]("p1")
      parser.parseResult(Seq("1.0"))
      parser.parseErrors ==> 1
    }
    test("float") {
      val parser = new TestParser
      parser.requiredParam[Double]("p1")
      parser.parseResult(Seq("1.0"))
      parser.parseErrors ==> 0
    }
    test("float is int") {
      val parser = new TestParser
      parser.requiredParam[Double]("p1")
      parser.parseResult(Seq("1"))
      parser.parseErrors ==> 0
    }
    test("option") {
      val parser = new TestParser
      val p = parser.requiredParam[Option[Int]]("p1")
      parser.parseResult(Seq("1"))
      parser.parseErrors ==> 0
      p() ==> Some(1)
    }
    test("missing option") {
      val parser = new TestParser
      val p = parser.requiredParam[Option[Int]]("p1")
      parser.parseResult(Seq())
      parser.parseErrors ==> 0
      // the type does not affect the parser; a missing param is still a missing param
      parser.missing ==> 1
    }
    test("collection") {
      val parser = new TestParser
      val p = parser.requiredParam[Seq[String]]("-p")
      parser.parseResult(Seq("-p", "a,b,c", "-p", "c,d,e")) ==> ArgParser.Success
      p() ==> Seq("c", "d", "e")
    }
    test("repeated collection") {
      val parser = new TestParser
      val p = parser.repeatedParam[Seq[String]]("-p")
      parser.parseResult(Seq("-p", "a,b,c", "-p", "c,d,e")) ==> ArgParser.Success
      p() ==> List(List("a", "b", "c"), List("c", "d", "e"))
    }
    test("key-value") {
      val parser = new TestParser
      val p = parser.repeatedParam[(String, Int)]("-p")
      parser.parseResult(Seq("-p=a=1", "-p", "c=2")) ==> ArgParser.Success
      p() ==> List("a" -> 1, "c" -> 2)
    }
  }

}
