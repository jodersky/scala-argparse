package cmdr


class Settings {

  var foo: Boolean = _
  var bar: Int = _
  var baz: Int = 0

  var enableExperimental = false

  object http {
    var port = 0
    var host = ""

    object yo {
      var aA = 1
      var b = 2
    }
  }

}

object Test {

  // def main(args: Array[String]): Unit = {
  //   val s = Settings()
  //   println(s.foo)

  //   val pdefs = Ap().settings(s)
  //   cmdr.Parser.parse(pdefs, args, s => println("unknown arg: " + s))

  //   println(s.foo)
  // }


  def main(args: Array[String]): Unit = {

    val parser = cmdr.ArgParser2()
    def foo = parser.param("--foo")
    parser.parse(args)

    println(foo)

  }

}

class Config {

  def foo: Int = parser.param

  def foo: Int = {
    parser.getArg("--foo")
  }


  def parse()

}
