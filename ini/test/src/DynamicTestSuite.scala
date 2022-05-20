import utest._, framework._

trait DynamicTestSuite extends TestSuite {
  import argparse.ini

  private val checks = IndexedSeq.newBuilder[(String, () => Unit)]

  protected def test(name: String)(fct: => Unit): Unit = {
    checks += (name -> (() => fct))
  }

  final override def tests: Tests = {
    val ts = checks.result()
    if (ts.isEmpty) {
      Tests.apply(())
    } else {
      val names = Tree("", ts.map((name, _) => Tree(name)): _*)
      val thunks = new TestCallTree(Right(ts.map((_, method) => new TestCallTree(Left(method())))))
      Tests.apply(names, thunks)
    }
  }

  protected def testAll(dirName: String)(action: os.Path => Unit) = {
    val inputDir = os.pwd / "ini" / "test" / "resources" / dirName
    for (inFile <- os.list(inputDir) if inFile.ext == "ini") test(inFile.baseName) {
      action(inFile)
    }
  }

}
