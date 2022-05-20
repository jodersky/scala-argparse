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
      val names = Tree("", ts.map(t => Tree(t._1)): _*)
      val thunks = new TestCallTree(Right(ts.map( t => new TestCallTree(Left(t._2())))))
      Tests.apply(names, thunks)
    }
  }

  protected def testAll(dirName: String, filter: os.Path => Boolean = _.ext == "ini")(action: os.Path => Unit) = {
    val inputDir = os.pwd / "ini" / "test" / "resources" / dirName
    for (inFile <- os.list(inputDir) if filter(inFile)) test(inFile.baseName) {
      action(inFile)
    }
  }

}
