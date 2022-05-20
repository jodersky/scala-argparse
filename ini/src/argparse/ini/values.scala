package argparse.ini

import scala.collection.mutable

case class Position(file: String, line: Int, col: Int) {
  override def toString = s"$file:$line:$col"
}

sealed trait Value {
  private var _pos: Position = _
  def pos = if (_pos == null) throw new RuntimeException("no position attached to this value") else _pos
  private [argparse] def setPos(p: Position): this.type = {
    _pos = p
    this
  }

  def str = this match {
    case Str(value) => value
    case other => throw Value.InvalidData(this, s"Expected ini.Str but found ${other.getClass().getName}")
  }

  def obj = this match {
    case Obj(value) => value
    case other => throw Value.InvalidData(this, s"Expected ini.Obj but found ${other.getClass().getName}")
  }

}

object Value {
  case class InvalidData(data: Value, msg: String)
    extends Exception(msg)

  // case class LookupException(root: Obj, msg: String)
  //   extends Exception(msg)
}

case class Str(value: String) extends Value
case class Obj(value: mutable.LinkedHashMap[String, Value]) extends Value {

  def lookup(segments: Seq[String]): Option[Value] = {
    var section = this
    val it = segments.init.iterator
    val processed = mutable.ListBuffer.empty[String]
    while (it.hasNext) {
      val segment = it.next()
      section.value.get(segment) match {
        case None => return None
        case Some(s: Obj) =>
          section = s
          processed += segment
        case Some(other) =>
          // throw Value.LookupException(
          //   this,
          //   s"conflict: expected a section at ${processed.mkString(".")} but " +
          //   s"found a ${other.getClass().toString()} (defined at ${other.pos})"
          // )
          return None
      }
    }
    section.value.get(segments.last)
  }

  def lookup(path: String): Option[Value] = lookup(path.split('.').toSeq)

}
object Obj {
  import scala.language.implicitConversions
  implicit def from(items: IterableOnce[(String, Value)]): Obj = {
    Obj(mutable.LinkedHashMap(items.iterator.toSeq:_*))
  }
  def apply[V](item: (String, V),
                        items: (String, Value)*)(implicit conv: V => Value): Obj = {
    val map = new mutable.LinkedHashMap[String, Value]()
    map.put(item._1, conv(item._2))
    for (i <- items) map.put(i._1, i._2)
    Obj(map)
  }
  def apply(): Obj = Obj(new mutable.LinkedHashMap[String, Value]())
}
