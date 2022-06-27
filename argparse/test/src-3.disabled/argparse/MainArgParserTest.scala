package argparse

import utest._

object MainArgParserTest extends TestSuite {

  object app {

    var x: String = _
    var y: Int = _
    var z: Seq[String] = _

    @argparse.main
    def run(x: String, y: Int = 2, z: Seq[String]) = {
      this.x = x
      this.y = y
      this.z = z
    }

    def entrypoint(args: Array[String]) = argparse.parseOrExit(args)
  }

  val tests = Tests {
    test("empty") {
      app.entrypoint(Array("x", "--y", "2", "a", "b", "c"))
      app.x ==> "x"
      app.y ==> 2
      app.z ==> Seq("a", "b", "c")
    }
  }

}
