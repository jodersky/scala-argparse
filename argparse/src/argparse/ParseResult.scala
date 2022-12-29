package argparse

sealed trait ParseResult
object ParseResult {

  /** Parsing succeeded. Arguments are available. */
  case object Success extends ParseResult

  /** There was an error during parsing. Arguments are not available. */
  case object Error extends ParseResult

  /** Parsing signalled an early exit. This means that there wasn't an error,
    * but that not all arguments were parsed. This occurs if one of the
    * arguments requested an early exit after some side-effect (for example,
    * `--help` will print a help message and then signal an early exit).
    * Arguments are not available. */
  case object EarlyExit extends ParseResult

}
