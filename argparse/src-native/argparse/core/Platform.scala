package argparse.core

object Platform {
  import scalanative.unsafe._
  import scalanative.posix.sys.types

  @extern
  object unistd {
    def isatty(fd: CInt): CInt = extern
  }

  def isConsole(): Boolean = unistd.isatty(1) == 1
}
