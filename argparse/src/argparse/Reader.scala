package argparse

import scala.annotation.implicitNotFound
import java.nio.file.InvalidPathException
import java.io.InputStream
import java.io.OutputStream

/** A typeclass that defines how to convert a string from a single command line
  * argument to a given type.
  */
@implicitNotFound(
  "No argparse.Reader[${A}] found. A reader is required to parse a command line argument from a string to a ${A}. " +
  "Please define a given argparse.Reader[$A]."
)
trait Reader[A] {

  /** Either convert the string to A or return a failure message.
    *
    * Note that throwing an exception from a reader will cause the parser
    * to crash, leading to a horrible user experience.
    */
  def read(a: String): Reader.Result[A]

  /** Compute available shell completions starting with a given string. This is
    * used by embedded bash completion, where the user program is responsible
    * for generating completions.
    */
  def completer: String => Seq[String] = _ => Seq.empty

  /** A completer for bash. This is used by standalone bash completion, where a
    * bash script generates completion, without the involvement of the the user
    * program.
    *
    * If your program is implemented with Scala on the JVM, the startup time is
    * considerable and hence standalone completion should be preferred for a
    * snappy user experience.
    */
  def bashCompleter: Reader.BashCompleter = Reader.BashCompleter.Empty
}

trait LowPrioReaders {
  import Reader.Result
  import Reader.Error
  import Reader.Success

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
  }
}

object Reader extends LowPrioReaders {

  sealed trait Result[+A]
  case class Success[A](value: A) extends Result[A]
  case class Error(message: String) extends Result[Nothing]

  sealed trait BashCompleter
  object BashCompleter {
    case object Empty extends BashCompleter // no completion
    case class Fixed(alternatives: Set[String]) extends BashCompleter // completion picked from a fixed set of words
    case object Default extends BashCompleter // default bash completion (uses paths)
  }

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

  val pathCompleter: String => Seq[String] = (prefix: String) => {
    import java.nio.file.{Files, Path, Paths}

    try {
      val completions = collection.mutable.ListBuffer.empty[String]
      val path = Paths.get(prefix)

      def addListing(dir: Path) = {
        val children = Files.list(dir).iterator()
        while (children.hasNext()) {
          val path = children.next()
          if (path.toString.startsWith(prefix)) {
            if (Files.isDirectory(path)) {
              completions += s"$path/"
            } else {
              completions += s"$path "
            }
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
    override val bashCompleter = BashCompleter.Default
  }

  implicit object FilePathReader extends FsPathReader[os.FilePath] {
    def read(a: String) =
      try {
        Success(os.FilePath(a))
      } catch {
        case _: IllegalArgumentException =>
          Error(s"'$a' is not a valid path")
      }
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
        Success(java.nio.file.Paths.get(a))
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
    override def completer =
      prefix => Seq("true", "false").filter(_.startsWith(prefix))
  }

  private def colonSeparatedReader[E, Col <: Iterable[E]](
    implicit elementReader: Reader[E],
    factory: collection.Factory[E, Col]
  ): Reader[Col] = new Reader[Col] {
    def read(a: String) = {
      val elems: List[Result[E]] =
        a.split(":").toList.map(elementReader.read(_))

      elems.find(_.isInstanceOf[Error]) match {
        case Some(err) => err.asInstanceOf[Error]
        case None =>
          Success(elems.map(_.asInstanceOf[Success[E]].value).to(factory))
      }
    }
  }

  implicit def FilePathCollectionReader[Col <: Iterable[os.FilePath]]
    (implicit factory: collection.Factory[os.FilePath, Col]): Reader[Col] =
      colonSeparatedReader[os.FilePath, Col]
  implicit def PathCollectionReader[Col <: Iterable[os.Path]]
    (implicit factory: collection.Factory[os.Path, Col]): Reader[Col] =
      colonSeparatedReader[os.Path, Col]
  implicit def RelPathCollectionReader[Col <: Iterable[os.RelPath]]
    (implicit factory: collection.Factory[os.RelPath, Col]): Reader[Col] =
      colonSeparatedReader[os.RelPath, Col]
  implicit def SubPathCollectionReader[Col <: Iterable[os.SubPath]]
    (implicit factory: collection.Factory[os.SubPath, Col]): Reader[Col] =
      colonSeparatedReader[os.SubPath, Col]
  implicit def JPathCollectionReader[Col <: Iterable[java.nio.file.Path]]
    (implicit factory: collection.Factory[java.nio.file.Path, Col]): Reader[Col] =
      colonSeparatedReader[java.nio.file.Path, Col]
  implicit def JFileCollectionReader[Col <: Iterable[java.io.File]]
    (implicit factory: collection.Factory[java.io.File, Col]): Reader[Col] =
      colonSeparatedReader[java.io.File, Col]

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
    override def completer = elementReader.completer
  }
  implicit object InputStreamReader extends Reader[() => java.io.InputStream] {
    override val completer = pathCompleter
    def read(a: String): Result[() => InputStream] = {
      if (a == "-") Success(() => System.in)
      else try {
        Success(() => java.nio.file.Files.newInputStream(java.nio.file.Paths.get(a)))
      } catch {
        case e: Exception => Error(e.getMessage())
      }
    }
  }
  implicit object OutputStreamReader extends Reader[() => java.io.OutputStream] {
    override val completer = pathCompleter
    def read(a: String): Result[() => OutputStream] = {
      if (a == "-") Success(() => System.out)
      else try {
        Success(() => java.nio.file.Files.newOutputStream(java.nio.file.Paths.get(a)))
      } catch {
        case e: Exception => Error(e.getMessage())
      }
    }
  }
  implicit object ReadableReader extends Reader[geny.Readable] {
    override val completer = pathCompleter
    def read(a: String): Result[geny.Readable] = InputStreamReader.read(a) match {
      case Success(open) =>
        Success(
          new geny.Readable {
            def readBytesThrough[A](f: java.io.InputStream => A): A = {
              val stream = open()
              try f(stream) finally stream.close()
            }
          }
        )
      case Error(msg) => Error(msg)
    }
  }
  implicit object DurationReader extends Reader[scala.concurrent.duration.Duration] {
    def read(a: String) = try {
      Success(scala.concurrent.duration.Duration.create(a))
    } catch {
      case _: NumberFormatException => Error(s"'$a' is not a valid duration")
    }
  }
  implicit object FiniteDurationReader extends Reader[scala.concurrent.duration.FiniteDuration] {
    def read(a: String) = DurationReader.read(a) match {
      case Success(f: scala.concurrent.duration.FiniteDuration) => Success(f)
      case Success(f: scala.concurrent.duration.Duration) =>
        Error(s"expected a finite duration, but '$a' is infinite")
      case Error(msg) => Error(msg)
    }
  }
  implicit object InstantReader extends Reader[java.time.Instant] {
    def read(a: String) = try {
      Success(java.time.Instant.parse(a))
    } catch {
      case ex: java.time.format.DateTimeParseException =>
        Error(s"can not parse $a as an instant in time. The format must follow 'YYYY-MM-DDThh:mm:ss[.S]Z'. Note that the 'T' is literal and the time zone Z must be given.")
    }
  }
  implicit object ZonedDateTimeReader extends Reader[java.time.ZonedDateTime] {
    def read(a: String) = try {
      Success(java.time.ZonedDateTime.parse(a))
    } catch {
      case ex: java.time.format.DateTimeParseException =>
        Error(s"can not parse $a as a zoned date and time")
    }
  }
  implicit object LocalDateTimeReader extends Reader[java.time.LocalDateTime] {
    def read(a: String) = try {
      Success(java.time.LocalDateTime.parse(a))
    } catch {
      case ex: java.time.format.DateTimeParseException =>
        Error(s"can not parse $a as a local date and time")
    }
  }
  implicit object LocalDateReader extends Reader[java.time.LocalDate] {
    def read(a: String) = try {
      Success(java.time.LocalDate.parse(a))
    } catch {
      case ex: java.time.format.DateTimeParseException =>
        Error(s"can not parse $a as a local date")
    }
  }
  implicit object LocalTime extends Reader[java.time.LocalTime] {
    def read(a: String) = try {
      Success(java.time.LocalTime.parse(a))
    } catch {
      case ex: java.time.format.DateTimeParseException =>
        Error(s"can not parse $a as a local time")
    }
  }
  implicit object RangeReader extends Reader[Range] {
    def read(str: String) = str.split("\\.\\.") match {
      case Array(from, to) =>
        try {
          argparse.Reader.Success(from.toInt to to.toInt)
        } catch {
          case _: Exception => argparse.Reader.Error(s"$str must be a numeric range")
        }
      case _ => argparse.Reader.Error(s"expected 'from..to', found: $str")
    }
  }
}
