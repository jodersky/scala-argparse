package argparse

sealed trait BashCompleter
object BashCompleter {

  /** No completion */
  case object Empty extends BashCompleter

  /** Completion picked from a fixed set of words */
  case class Fixed(alternatives: Set[String]) extends BashCompleter

  /** Default bash completion (uses paths) */
  case object Default extends BashCompleter

  /** A plain bash sniipet that is executed during completion.
    *
    * This should typically either set `COMPREPLY` or call `compopt`.
    */
  case class Raw(script: String) extends BashCompleter
}
