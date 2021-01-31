import utest._

object Test extends TestSuite {

  val tests = Tests {
    test("clone")(
      Main.main(Array("--host", "0.0.0.0", "/a/b/c"))
    )
  }

}
