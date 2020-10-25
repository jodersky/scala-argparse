import utest._

object Test extends TestSuite {

  val tests = Tests {
    test("demo")(
      Main.main(Array("--port", "80", "--flag", "/path"))
    )
    test("nested")(
      Main.main(Array("/path", "--port=80"))
    )
  }

}
