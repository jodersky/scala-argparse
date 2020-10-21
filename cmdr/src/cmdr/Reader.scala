package cmdr

import scala.annotation.implicitNotFound
import scala.collection.Factory
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
}

object Reader {
  implicit object StringReader extends Reader[String] {
    def read(a: String) = Right(a)
  }

  implicit def IntegralReader[N](implicit numeric: Integral[N]): Reader[N] =
    new Reader[N] {
      def read(a: String) = numeric.parseString(a) match {
        case None    => Left(s"'$a' is not an integral number")
        case Some(n) => Right(n)
      }
    }

  implicit def FractionalReader[N](implicit numeric: Fractional[N]): Reader[N] =
    new Reader[N] {
      def read(a: String) = numeric.parseString(a) match {
        case None    => Left(s"'$a' is not a number")
        case Some(n) => Right(n)
      }
    }

  implicit object PathReader extends Reader[os.Path] {
    def read(a: String) =
      try {
        Right(os.Path(a, os.pwd))
      } catch {
        case _: IllegalArgumentException =>
          Left(s"'$a' is not a valid path")
      }
  }
  implicit object SubPathReader extends Reader[os.SubPath] {
    def read(a: String) =
      try {
        Right(os.SubPath(a))
      } catch {
        case _: IllegalArgumentException =>
          Left(s"'$a' is not a relative child path")
      }
  }
  implicit object RelPathReader extends Reader[os.RelPath] {
    def read(a: String) =
      try {
        Right(os.RelPath(a))
      } catch {
        case _: IllegalArgumentException =>
          Left(s"'$a' is not a relative path")
      }
  }
  implicit object JavaPathReader extends Reader[java.nio.file.Path] {
    def read(a: String) =
      try {
        Right(java.nio.file.Path.of(a))
      } catch {
        case _: InvalidPathException => Left(s"'$a' is not a path")
      }
  }
  implicit object JavaFileReader extends Reader[java.io.File] {
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
      factory: Factory[Elem, Col[Elem]]
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
          for (k1 <- kr.read(k); v1 <- vr.read(v)) yield (k1 -> v1)
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
  }

}
