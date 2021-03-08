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

  /** Show a given value as a string. This is used in help dialogs to display
    * default values.
    **/
  def show(a: A): String

  /** Compute available bash completions starting with a given string. */
  def completer: String => Seq[String] = _ => Seq.empty
}

object Reader {

  sealed trait Result[+A]
  case class Success[A](value: A) extends Result[A]
  case class Error(message: String) extends Result[Nothing]

  implicit object StringReader extends Reader[String] {
    def read(a: String) = Success(a)
    def show(a: String): String = a
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
      def show(a: N): String = a.toString
    }

  implicit object FloatReader extends Reader[Float] {
    def read(a: String) =
      try {
        Success(a.toFloat)
      } catch {
        case _: NumberFormatException => Error(s"'$a' is not a number")
      }
    def show(a: Float): String = a.toString
  }

  implicit object DoubleReader extends Reader[Double] {
    def read(a: String) =
      try {
        Success(a.toDouble)
      } catch {
        case _: NumberFormatException => Error(s"'$a' is not a number")
      }
    def show(a: Double): String = a.toString
  }

  val pathCompleter: String => Seq[String] = (prefix: String) => {
    import java.nio.file.{Files, Path, Paths}

    try {
      val completions = collection.mutable.ListBuffer.empty[String]
      val path = Paths.get(prefix)

      def addListing(dir: Path) = Files.list(dir).forEach { path =>
        if (path.toString.startsWith(prefix)) {
          if (Files.isDirectory(path)) {
            completions += s"$path/"
          } else {
            completions += s"$path "
          }
        }
      }

      if (Files.isDirectory(path) && prefix.endsWith("/")) {
        addListing(path)
      } else {
        path.getParent() match {
          case null => addListing(Paths.get(""))
          case dir  => addListing(dir)
        }
      }

      completions.result()
    } catch {
      case _: Exception => Seq()
    }
  }

  trait FsPathReader[A] extends Reader[A] {
    override val completer = pathCompleter
  }

  implicit object PathReader extends FsPathReader[os.Path] {
    def read(a: String) =
      try {
        Success(os.Path(a, os.pwd))
      } catch {
        case _: IllegalArgumentException =>
          Error(s"'$a' is not a valid path")
      }
    def show(a: os.Path): String = a.toString
  }
  implicit object SubPathReader extends FsPathReader[os.SubPath] {
    def read(a: String) =
      try {
        Success(os.SubPath(a))
      } catch {
        case _: IllegalArgumentException =>
          Error(s"'$a' is not a relative child path")
      }
    def show(a: os.SubPath): String = a.toString
  }
  implicit object RelPathReader extends FsPathReader[os.RelPath] {
    def read(a: String) =
      try {
        Success(os.RelPath(a))
      } catch {
        case _: IllegalArgumentException =>
          Error(s"'$a' is not a relative path")
      }
    def show(a: os.RelPath): String = a.toString
  }
  implicit object JavaPathReader extends FsPathReader[java.nio.file.Path] {
    def read(a: String) =
      try {
        Success(java.nio.file.Paths.get(a))
      } catch {
        case _: InvalidPathException => Error(s"'$a' is not a path")
      }
    def show(a: java.nio.file.Path): String = a.toString
  }
  implicit object JavaFileReader extends FsPathReader[java.io.File] {
    def read(a: String) =
      try {
        Success(new java.io.File(a))
      } catch {
        case _: Exception => Error(s"'$a' is not a path")
      }
    def show(a: java.io.File): String = a.toString
  }
  implicit object BooleanReader extends Reader[Boolean] {
    def read(a: String): Result[Boolean] = a match {
      case "true"  => Success(true)
      case "false" => Success(false)
      case _       => Error(s"'$a' is not either 'true' or 'false'")
    }
    def show(a: Boolean): String = a.toString
    override def completer =
      prefix => Seq("true", "false").filter(_.startsWith(prefix))
  }
  implicit def CollectionReader[Elem, Col[Elem] <: Iterable[Elem]](
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
    def show(a: Col[Elem]): String = a.map(elementReader.show(_)).mkString(",")
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
    def show(a: (K, V)): String = kr.show(a._1) + "=" + vr.show(a._2)
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
    def show(a: Option[A]): String = a match {
      case None => ""
      case Some(value) => elementReader.show(value)
    }
    override def completer = elementReader.completer
  }

}
