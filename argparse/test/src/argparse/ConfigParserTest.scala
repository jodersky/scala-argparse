package argparse

import utest._

object ConfigParserTest extends TestSuite {
  val tests = Tests{
    test("basic") {
      ini.read("hello=world") ==> ini.Section("hello" -> ini.Str("world"))
      ini.read("[ section1 ]") ==> ini.Section("section1" -> ini.Section())
      ini.read("") ==> ini.Section()
    }
    test("file") {
      ini.read(os.pwd / "argparse" / "test" / "resources" / "ini1") ==>
        ini.Section(
          "global_key" -> ini.Str("hello world"),
          "section1" -> ini.Section(
            "foo" -> ini.Str("a"),
            "bar" -> ini.Str("a=b"),
            "test" -> ini.Section(
              "foo" -> ini.Str("a"),
              "bar" -> ini.Str("a=b"),
            )
          ),
          "section2" -> ini.Section(),
          "section3" -> ini.Section(
            "a" -> ini.Str("1234 hello world")
          )
        )
    }
    test("errors") {
      intercept[ini.ParseException](ini.read("a"))
      intercept[ini.ParseException](ini.read("["))
      intercept[ini.ParseException](ini.read("a["))
      intercept[ini.ParseException](ini.read("[a"))
      intercept[ini.ParseException](ini.read("]"))
      intercept[ini.ParseException](ini.read("[a]", "a=1"))
      intercept[ini.ParseException](ini.read("a=1", "[a]"))
    }
  }

}
