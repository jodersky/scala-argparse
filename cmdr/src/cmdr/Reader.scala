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
  def read(a: String): Either[String, A]

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
  implicit object StringReader extends Reader[String] {
    def read(a: String) = Right(a)
  }

  implicit def IntegralReader[N](implicit numeric: Integral[N]): Reader[N] =
    new Reader[N] {
      def read(a: String) = try {
        Right(numeric.fromInt(a.toInt))
      } catch {
        case _: NumberFormatException => Left(s"'$a' is not an integral number")
      }
    }

  implicit object FloatReader extends Reader[Float] {
    def read(a: String) = try {
      Right(a.toFloat)
    } catch {
      case _: NumberFormatException => Left(s"'$a' is not a number")
    }
  }

  implicit object DoubleReader extends Reader[Double] {
    def read(a: String) = try {
      Right(a.toDouble)
    } catch {
      case _: NumberFormatException => Left(s"'$a' is not a number")
    }
  }

  trait FsPathReader[A] extends Reader[A] {
    override def completer: String = "compopt -o default"
  }

  implicit object PathReader extends FsPathReader[os.Path] {
    def read(a: String) =
      try {
        Right(os.Path(a, os.pwd))
      } catch {
        case _: IllegalArgumentException =>
          Left(s"'$a' is not a valid path")
      }
  }
  implicit object SubPathReader extends FsPathReader[os.SubPath] {
    def read(a: String) =
      try {
        Right(os.SubPath(a))
      } catch {
        case _: IllegalArgumentException =>
          Left(s"'$a' is not a relative child path")
      }
  }
  implicit object RelPathReader extends FsPathReader[os.RelPath] {
    def read(a: String) =
      try {
        Right(os.RelPath(a))
      } catch {
        case _: IllegalArgumentException =>
          Left(s"'$a' is not a relative path")
      }
  }
  implicit object JavaPathReader extends FsPathReader[java.nio.file.Path] {
    def read(a: String) =
      try {
        Right(java.nio.file.Path.of(a))
      } catch {
        case _: InvalidPathException => Left(s"'$a' is not a path")
      }
  }
  implicit object JavaFileReader extends FsPathReader[java.io.File] {
    def read(a: String) =
      try {
        Right(new java.io.File(a))
      } catch {
        case _: Exception => Left(s"'$a' is not a path")
      }
  }
  implicit object BooleanReader extends Reader[Boolean] {
    def read(a: String): Either[String, Boolean] = a match {
      case "true"  => Right(true)
      case "false" => Right(false)
      case _       => Left(s"'$a' is not either 'true' or 'false'")
    }
  }
  implicit def CollectionReader[Elem, Col[Elem]](
      implicit elementReader: Reader[Elem],
      factory: collection.Factory[Elem, Col[Elem]]
  ): Reader[Col[Elem]] = new Reader[Col[Elem]] {
    def read(a: String) = {
      val elems: List[Either[String, Elem]] =
        a.split(",").toList.map(elementReader.read(_))
      if (elems.exists(_.isLeft)) {
        val Left(err) = elems.find(_.isLeft).get
        Left(err)
      } else {
        Right(elems.map(_.getOrElse(sys.error("match error"))).to(factory))
      }
    }
  }
  implicit def Mapping[K, V](
      implicit kr: Reader[K],
      vr: Reader[V]
  ): Reader[(K, V)] = new Reader[(K, V)] {
    def read(a: String): Either[String, (K, V)] = {
      a.split("=", 2) match {
        case Array(k, v) =>
          val k1 = kr.read(k)
          val v1 = vr.read(v)
          (k1, v1) match {
            case (Right(k2), Right(v2)) => Right((k2, v2))
            case (Left(msg), _) => Left(msg)
            case (Right(_), Left(msg)) => Left(msg)
          }
        case Array(k) => Left(s"expected value after key '$k'")
        case _        => Left(s"expected key=value pair")
      }
    }
  }
  implicit def OptionReader[A](
      implicit elementReader: Reader[A]
  ): Reader[Option[A]] = new Reader[Option[A]] {
    def read(a: String): Either[String, Option[A]] = {
      elementReader.read(a) match {
        case Left(message) => Left(message)
        case Right(value)  => Right(Some(value))
      }
    }
    override def completer: String = elementReader.completer
  }

}
