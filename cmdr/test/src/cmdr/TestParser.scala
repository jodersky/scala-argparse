package cmdr

object NullStream extends java.io.OutputStream {
  override def write(x: Int): Unit = ()
  override def write(x: Array[Byte]): Unit = ()
  override def write(x: Array[Byte], y: Int, z: Int): Unit = ()
}

class TestReporter extends ArgParser.Reporter {

  override val stdout = new java.io.PrintStream(NullStream)
  override val stderr = new java.io.PrintStream(NullStream)

  var missing = 0
  override def reportMissing(name: String): Unit = {
    missing += 1
  }
  var unknown = 0
  override def reportUnknown(name: String): Unit = unknown += 1
  var unknownCmds = 0
  override def reportUnknownCommand(
      actual: String,
      available: Seq[String]
  ): Unit = unknownCmds += 1
  var parseErrors = 0
  override def reportParseError(name: String, message: String): Unit =
    parseErrors += 1
  override def hasErrors =
    missing > 0 || unknown > 0 || unknownCmds > 0 || parseErrors > 0
}

class TestParser(
    version: String = "",
    override val reporter: TestReporter = new TestReporter(),
    env: Map[String, String] = Map.empty
) extends ArgParser("", "", version, reporter, env) {
  def missing = reporter.missing
  def unknown = reporter.unknown
  def unknownCmds = reporter.unknownCmds
  def parseErrors = reporter.parseErrors
}
