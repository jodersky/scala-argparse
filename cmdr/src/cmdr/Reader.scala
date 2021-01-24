package cmdr

import scala.annotation.implicitNotFound
import java.nio.file.InvalidPathException

/** A typeclass that defines how to convert a string from a single command line
  * argument to a given type.
  */
@implicitNotFound(
  "Don't know how to read a ${A} from a command line argument. Try implementing your own cmdr.Reader[$A]."
)
trait Reader[A] {

  /** Either convert the string to A or return a failure message.
    *
    * Note that throwing an exception from a reader will cause the parser
    * to crash, leading to a horrible user experience.
    */
  def read(a: String): Reader.Result[A]

  /** Bash completion snippet for arguments of this type.
    *
    * Must be valid bash. The variable "$cur" may be assumed to contain the
    * current word being completed.
    *
    * Typically this would set COMPREPLY of compopt. E.g.
    *
    * {{{
    * COMPREPLY=( $(compgen -W 'foo bar baz' -- "$cur") )
    * }}}
    *
    * - or -
    *
    * {{{
    * compopt -o default
    * }}}
    *
    * Leave blank for no completion.
    */
  def completer: String = ""
}

object Reader {

  sealed trait Result[+A]
  case class Success[A](value: A) extends Result[A]
  case class Error(message: String) extends Result[Nothing]

  implicit object StringReader extends Reader[String] {
    def read(a: String) = Success(a)
  }

  implicit def IntegralReader[N](implicit numeric: Integral[N]): Reader[N] =
    new Reader[N] {
      def read(a: String) =
        try {
          Success(numeric.fromInt(a.toInt))
        } catch {
          case _: NumberFormatException =>
            Error(s"'$a' is not an integral number")
        }
    }

  implicit object FloatReader extends Reader[Float] {
    def read(a: String) =
      try {
        Success(a.toFloat)
      } catch {
        case _: NumberFormatException => Error(s"'$a' is not a number")
      }
  }

  implicit object DoubleReader extends Reader[Double] {
    def read(a: String) =
      try {
        Success(a.toDouble)
      } catch {
        case _: NumberFormatException => Error(s"'$a' is not a number")
      }
  }

  trait FsPathReader[A] extends Reader[A] {
    override def completer: String = "compopt -o default"
  }

  implicit object PathReader extends FsPathReader[os.Path] {
    def read(a: String) =
      try {
        Success(os.Path(a, os.pwd))
      } catch {
        case _: IllegalArgumentException =>
          Error(s"'$a' is not a valid path")
      }
  }
  implicit object SubPathReader extends FsPathReader[os.SubPath] {
    def read(a: String) =
      try {
        Success(os.SubPath(a))
      } catch {
        case _: IllegalArgumentException =>
          Error(s"'$a' is not a relative child path")
      }
  }
  implicit object RelPathReader extends FsPathReader[os.RelPath] {
    def read(a: String) =
      try {
        Success(os.RelPath(a))
      } catch {
        case _: IllegalArgumentException =>
          Error(s"'$a' is not a relative path")
      }
  }
  implicit object JavaPathReader extends FsPathReader[java.nio.file.Path] {
    def read(a: String) =
      try {
        Success(java.nio.file.Path.of(a))
      } catch {
        case _: InvalidPathException => Error(s"'$a' is not a path")
      }
  }
  implicit object JavaFileReader extends FsPathReader[java.io.File] {
    def read(a: String) =
      try {
        Success(new java.io.File(a))
      } catch {
        case _: Exception => Error(s"'$a' is not a path")
      }
  }
  implicit object BooleanReader extends Reader[Boolean] {
    def read(a: String): Result[Boolean] = a match {
      case "true"  => Success(true)
      case "false" => Success(false)
      case _       => Error(s"'$a' is not either 'true' or 'false'")
    }
  }
  implicit def CollectionReader[Elem, Col[Elem]](
      implicit elementReader: Reader[Elem],
      factory: collection.Factory[Elem, Col[Elem]]
  ): Reader[Col[Elem]] = new Reader[Col[Elem]] {
    def read(a: String) = {
      val elems: List[Result[Elem]] =
        a.split(",").toList.map(elementReader.read(_))

      elems.find(_.isInstanceOf[Error]) match {
        case Some(err) => err.asInstanceOf[Error]
        case None =>
          Success(elems.map(_.asInstanceOf[Success[Elem]].value).to(factory))
      }
    }
  }
  implicit def Mapping[K, V](
      implicit kr: Reader[K],
      vr: Reader[V]
  ): Reader[(K, V)] = new Reader[(K, V)] {
    def read(a: String): Result[(K, V)] = {
      a.split("=", 2) match {
        case Array(k, v) =>
          val k1 = kr.read(k)
          val v1 = vr.read(v)
          (k1, v1) match {
            case (Success(k2), Success(v2)) => Success((k2, v2))
            case (Error(msg), _)            => Error(msg)
            case (Success(_), Error(msg))   => Error(msg)
          }
        case Array(k) => Error(s"expected value after key '$k'")
        case _        => Error(s"expected key=value pair")
      }
    }
  }
  implicit def OptionReader[A](
      implicit elementReader: Reader[A]
  ): Reader[Option[A]] = new Reader[Option[A]] {
    def read(a: String): Result[Option[A]] = {
      elementReader.read(a) match {
        case Error(message) => Error(message)
        case Success(value) => Success(Some(value))
      }
    }
    override def completer: String = elementReader.completer
  }

}
