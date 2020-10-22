package cmdr

class TestParser extends ArgParser("", "", "") {
  var missing = 0
  override def reportMissing(name: String): Unit = missing += 1
  var unknown = 0
  override def reportUnknown(name: String): Unit = unknown += 1
  var unknownCmds = 0
  override protected def reportUnknownCommand(actual: String, available: Seq[String]): Unit = unknownCmds += 1
  var parseErrors = 0
  override def reportParseError(
      name: String,
      message: String
  ): Unit = parseErrors += 1
  override def check(): Boolean =
    missing == 0 && unknown == 0 && unknownCmds == 0 && parseErrors == 0
}
