// object NullStream extends java.io.OutputStream {
//   override def write(x: Int): Unit = ()
//   override def write(x: Array[Byte]): Unit = ()
//   override def write(x: Array[Byte], y: Int, z: Int): Unit = ()
// }

class TestParser extends argparse.default.ArgumentParser(
  description = "",
  helpFlags = Seq("--help"),
  bashCompletionFlags = Seq("--bash-completion")
) {
  var missing = 0
  override protected def reportMissing(name: String): Unit = {
    missing += 1
  }

  var unknown = 0
  override protected def reportUnknown(name: String): Unit = unknown += 1

  var unknownCmds = 0
  override protected def reportUnknownCommand(
      actual: String
  ): Unit = unknownCmds += 1

  var parseErrors = 0
  override def reportParseError(name: String, message: String): Unit =
    parseErrors += 1

  override def hasErrors =
    missing > 0 || unknown > 0 || unknownCmds > 0 || parseErrors > 0
}
