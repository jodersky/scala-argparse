package argparse

/** Properties of the current terminal. */
object term {

  /** Get number of rows in the current terminal.
    *
    * This will return None if the number of rows cannot be determined,
    * for example on an unsupported operating system.
    */
  def rows: Option[Int] = try {
    val result = os.proc("stty", "-a").call(stdin = os.Path("/dev/tty")).out.lines().head
    """rows (\d+)""".r.findFirstMatchIn(result).map(_.group(1).toInt)
  } catch {
    case _: Exception => None
  }

  /** Get number of columns in the current terminal.
    *
    * This will return None if the number of columns cannot be determined,
    * for example on an unsupported operating system.
    */
  def cols: Option[Int] = try {
    val result = os.proc("stty", "-a").call(stdin = os.Path("/dev/tty")).out.lines().head
    """columns (\d+)""".r.findFirstMatchIn(result).map(_.group(1).toInt)
  } catch {
    case _: Exception => None
  }

}
