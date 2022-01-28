import utest._

object Test extends TestSuite {

  val tests = Tests {
    test("clone")(
      Main.main(Array("clone", "https://github.com/jodersky/argparse"))
    )
    test("remote")(
      Main.main(Array("--git-dir=foo", "remote", "set-url", "foo", "bar"))
    )
  }

}
