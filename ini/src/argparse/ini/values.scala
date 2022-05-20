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

  // def str = this match {
  //   case Str(value) => value
  //   case _ => throw Value.InvalidData("Expected string. Found")
  // }

  // def obj = this match {
  //   case Obj(value) => value
  //   case _ => throw Value.InvalidData()
  // }

}

object Value {
  case class InvalidData(data: Value, msg: String)
    extends Exception(s"$msg (data: $data)")
}

case class Str(value: String) extends Value
case class Obj(value: mutable.LinkedHashMap[String, Value]) extends Value
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
