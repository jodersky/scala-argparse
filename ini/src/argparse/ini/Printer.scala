package argparse.ini

class FlatPrinter(stream: java.io.OutputStream, indentation: Int) {
  private val lp = new java.io.PrintStream(stream)

  private def _print(root: Obj): Unit = {
    var empty = true
    var path: List[String] = Nil
    val remaining = collection.mutable.Stack.empty[(List[String], Obj)]
    var curr: Obj = root
    while (curr != null) {
      val objs = collection.mutable.ListBuffer.empty[(List[String], Obj)]
      curr.value.foreach{ case (key, value) =>
        empty = false
        value match {
          case s: Str =>
            if (curr != root) lp.print(" " * indentation)
            lp.print(key)
            lp.print("=")
            lp.println(s.str)
          case o: Obj => objs += ((path :+ key).toList -> o)
        }
      }
      remaining.pushAll(objs.reverse)

      if (remaining.isEmpty) {
        curr = null
      } else {
        if (!empty) lp.println("")
        val (newpath, obj) = remaining.pop()
        path = newpath
        curr = obj
        lp.print("[")
        lp.print(newpath.mkString("."))
        lp.println("]")
      }
    }
  }

  def print(v: Value): Unit = v match {
    case o: Obj => _print(o)
    case s: Str => lp.println(s) // this case shouldn't really ever happen
  }

}
