package argparse

sealed trait BashCompleter
object BashCompleter {
  case object Empty extends BashCompleter // no completion
  case class Fixed(alternatives: Set[String]) extends BashCompleter // completion picked from a fixed set of words
  case object Default extends BashCompleter // default bash completion (uses paths)
}
