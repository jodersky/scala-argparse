package argparse

package object ini {
  import scala.collection.mutable

  case class Position(file: String, line: Int, col: Int) {
    override def toString = s"$file:$line:$col"
  }

  sealed trait Value {
    private var _pos: Position = _
    def pos = if (_pos == null) throw new RuntimeException("no position attached to this value") else _pos
    def setPos(p: Position): this.type = {
      _pos = p
      this
    }
  }
  case class Str(value: String) extends Value
  case class Section(value: mutable.LinkedHashMap[String, Value]) extends Value
  object Section {
    import scala.language.implicitConversions
    implicit def from(items: IterableOnce[(String, Value)]): Section = {
      Section(mutable.LinkedHashMap(items.iterator.toSeq:_*))
    }
    def apply[V](item: (String, V),
                          items: (String, Value)*)(implicit conv: V => Value): Section = {
      val map = new mutable.LinkedHashMap[String, Value]()
      map.put(item._1, conv(item._2))
      for (i <- items) map.put(i._1, i._2)
      Section(map)
    }
    def apply(): Section = Section(new mutable.LinkedHashMap[String, Value]())
  }

  case class Parseable(readable: geny.Readable, filename: String)
  object Parseable {
    import scala.language.implicitConversions

    implicit def FileIsParseable(path: os.ReadablePath): Parseable = Parseable(os.read.stream(path), path.toString)
    implicit def ReadableIsParseable(readable: geny.Readable): Parseable = Parseable(readable, "<virtual>")
    implicit def ReadableIsParseable[T](t: T)(implicit mkReadable: T => geny.Readable): Parseable = Parseable(mkReadable(t), "<virtual>")
  }

  /** Priority-ordered input files. When config values will be
    * looked up, the entry first encountered in this list will be used.
    */
  def read(inputs: Parseable*): Section = {
    val parser = new ConfigParser()
    for (input <- inputs.reverse) {
      parser.parse(input.readable, input.filename)
    }
    //parser.parse(input.readable, input.filename)
    Section(parser.root)
  }

}
