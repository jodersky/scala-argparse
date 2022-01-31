package argparse

object NullStream extends java.io.OutputStream {
  override def write(x: Int): Unit = ()
  override def write(x: Array[Byte]): Unit = ()
  override def write(x: Array[Byte], y: Int, z: Int): Unit = ()
}

class TestParser extends ArgumentParser(
  "",
  true,
  true,
  new java.io.PrintStream(NullStream),
  new java.io.PrintStream(NullStream)
) {
  var missing = 0
  override protected def reportMissing(name: String): Unit = {
    missing += 1
  }

  var unknown = 0
  override protected def reportUnknown(name: String): Unit = unknown += 1

  var unknownCmds = 0
  override protected def reportUnknownCommand(
      actual: String,
      available: Seq[String]
  ): Unit = unknownCmds += 1

  var parseErrors = 0
  override protected[argparse] def reportParseError(name: String, message: String): Unit =
    parseErrors += 1

  override def hasErrors =
    missing > 0 || unknown > 0 || unknownCmds > 0 || parseErrors > 0
}
