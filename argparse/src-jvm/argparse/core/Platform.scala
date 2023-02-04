package argparse.core

object Platform {
  def isConsole(): Boolean = System.console() != null
}
