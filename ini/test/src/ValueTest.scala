import utest._
import argparse.ini

object ValueTest extends TestSuite {

  val tests = Tests{
    test("basic") {
      val section = ini.read("a=b")
      assert(section.obj("a").str == "b")
    }
    test("position") {
      val section = ini.read(" a=b" -> "noname")
      assert(section.pos.file == "noname")
      assert(section.pos.line == 1)
      assert(section.pos.col == 1)

      val v = section.obj("a")
      assert(v.str == "b")
      assert(v.pos.file == "noname")
      assert(v.pos.line == 1)
      assert(v.pos.col == 2)
    }
    test("section"){
      val section = ini.read(
        """|a=1
           |[b]
           |c=2
           |[d.e]
           |f=3
           |""".stripMargin
      )
      test("access") {
        assert(section.obj("a").str == "1")
        assert(section.obj("b").obj("c").str == "2")
        assert(section.obj("d").obj("e").obj("f").str == "3")
      }
      test("lookup") {
        assert(section.lookup("a").get.str == "1")

        assert(section.lookup("b").get.obj("c").str == "2")
        assert(section.lookup("b.c").get.str == "2")

        assert(section.lookup("d").get.obj("e").obj("f").str == "3")
        assert(section.lookup("d.e").get.obj("f").str == "3")
        assert(section.lookup("d.e.f").get.str == "3")

        assert(section.lookup("") == None)
        assert(section.lookup("a.b.c.d") == None)
        assert(section.lookup("x") == None)

        intercept[UnsupportedOperationException] {
          section.lookup(".")
        }
        intercept[UnsupportedOperationException] {
          section.lookup("..")
        }
      }
    }
    test("wrong type") {
      intercept[ini.Value.InvalidData] {
        ini.read("").str
      }
      intercept[ini.Value.InvalidData] {
        ini.read("a=b").obj("a").obj
      }
      intercept[ini.Value.InvalidData] {
        ini.read("[a]\na=b").obj("a").str
      }
    }
  }

}
