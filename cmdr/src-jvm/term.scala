package cmdr

/** Properties of the current terminal. */
object term {

  def rows: Option[Int] = try {
    val result = os.proc("stty", "-a").call(stdin = os.Path("/dev/tty")).out.lines().head
    """rows (\d+)""".r.findFirstMatchIn(result).map(_.group(1).toInt)
  } catch {
    case _: Exception => None
  }

  def cols: Option[Int] = try {
    val result = os.proc("stty", "-a").call(stdin = os.Path("/dev/tty")).out.lines().head
    """columns (\d+)""".r.findFirstMatchIn(result).map(_.group(1).toInt)
  } catch {
    case _: Exception => None
  }

}
