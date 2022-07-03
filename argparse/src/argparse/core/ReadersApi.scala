package argparse.core

trait ReadersApi extends LowPrioReaders { types: TypesApi =>
  import types.Reader.Result
  import types.Reader.Success
  import types.Reader.Error

  implicit def IntegralReader[N](implicit numeric: Integral[N]): Reader[N] =
    new Reader[N] {
      def read(a: String) =
        try {
          Success(numeric.fromInt(a.toInt))
        } catch {
          case _: NumberFormatException =>
            Error(s"'$a' is not an integral number")
        }
      def typeName: String = "int"
    }

  implicit object FloatReader extends Reader[Float] {
    def read(a: String) =
      try {
        Success(a.toFloat)
      } catch {
        case _: NumberFormatException => Error(s"'$a' is not a number")
      }
    def typeName: String = "float"
  }

  implicit object DoubleReader extends Reader[Double] {
    def read(a: String) =
      try {
        Success(a.toDouble)
      } catch {
        case _: NumberFormatException => Error(s"'$a' is not a number")
      }
    def typeName: String = "float"
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
    def typeName: String = "path"
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
    override def typeName: String = "subpath"
  }
  implicit object RelPathReader extends FsPathReader[os.RelPath] {
    def read(a: String) =
      try {
        Success(os.RelPath(a))
      } catch {
        case _: IllegalArgumentException =>
          Error(s"'$a' is not a relative path")
      }
    override def typeName: String = "relpath"
  }
  implicit object JavaPathReader extends FsPathReader[java.nio.file.Path] {
    def read(a: String) =
      try {
        Success(java.nio.file.Paths.get(a))
      } catch {
        case _: java.nio.file.InvalidPathException => Error(s"'$a' is not a path")
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
    override def typeName: String = "true|false"
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
    override def typeName: String = s"list of ${elementReader.typeName}s separated by ':'"
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
    def typeName = s"${kr.typeName}=${vr.typeName}"
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
    def typeName = elementReader.typeName
  }
  implicit object InputStreamReader extends Reader[() => java.io.InputStream] {
    override val completer = pathCompleter
    def read(a: String): Result[() => java.io.InputStream] = {
      if (a == "-") Success(() => System.in)
      else try {
        Success(() => java.nio.file.Files.newInputStream(java.nio.file.Paths.get(a)))
      } catch {
        case e: Exception => Error(e.getMessage())
      }
    }
    def typeName = "file|-"
  }
  implicit object OutputStreamReader extends Reader[() => java.io.OutputStream] {
    override val completer = pathCompleter
    def read(a: String): Result[() => java.io.OutputStream] = {
      if (a == "-") Success(() => System.out)
      else try {
        Success(() => java.nio.file.Files.newOutputStream(java.nio.file.Paths.get(a)))
      } catch {
        case e: Exception => Error(e.getMessage())
      }
    }
    def typeName = "file|-"
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
    def typeName = "file|-"
  }
  implicit object DurationReader extends Reader[scala.concurrent.duration.Duration] {
    def read(a: String) = try {
      Success(scala.concurrent.duration.Duration.create(a))
    } catch {
      case _: NumberFormatException => Error(s"'$a' is not a valid duration")
    }
    def typeName = "duration"
  }
  implicit object FiniteDurationReader extends Reader[scala.concurrent.duration.FiniteDuration] {
    def read(a: String) = DurationReader.read(a) match {
      case Success(f: scala.concurrent.duration.FiniteDuration) => Success(f)
      case Success(f: scala.concurrent.duration.Duration) =>
        Error(s"expected a finite duration, but '$a' is infinite")
      case Error(msg) => Error(msg)
    }
    def typeName = "duration"
  }
  implicit object InstantReader extends Reader[java.time.Instant] {
    def read(a: String) = try {
      Success(java.time.Instant.parse(a))
    } catch {
      case ex: java.time.format.DateTimeParseException =>
        Error(s"can not parse $a as an instant in time. The format must follow 'YYYY-MM-DDThh:mm:ss[.S]Z'. Note that the 'T' is literal and the time zone Z must be given.")
    }
    def typeName = "timestamp"
  }
  implicit object ZonedDateTimeReader extends Reader[java.time.ZonedDateTime] {
    def read(a: String) = try {
      Success(java.time.ZonedDateTime.parse(a))
    } catch {
      case ex: java.time.format.DateTimeParseException =>
        Error(s"can not parse $a as a zoned date and time")
    }
    def typeName = "timestamp"
  }
  implicit object LocalDateTimeReader extends Reader[java.time.LocalDateTime] {
    def read(a: String) = try {
      Success(java.time.LocalDateTime.parse(a))
    } catch {
      case ex: java.time.format.DateTimeParseException =>
        Error(s"can not parse $a as a local date and time")
    }
    def typeName = "local timestamp"
  }
  implicit object LocalDateReader extends Reader[java.time.LocalDate] {
    def read(a: String) = try {
      Success(java.time.LocalDate.parse(a))
    } catch {
      case ex: java.time.format.DateTimeParseException =>
        Error(s"can not parse $a as a local date")
    }
    def typeName = "local date"
  }
  implicit object LocalTime extends Reader[java.time.LocalTime] {
    def read(a: String) = try {
      Success(java.time.LocalTime.parse(a))
    } catch {
      case ex: java.time.format.DateTimeParseException =>
        Error(s"can not parse $a as a local time")
    }
    def typeName = "local time"
  }
  implicit object RangeReader extends Reader[Range] {
    def read(str: String) = str.split("\\.\\.") match {
      case Array(from, to) =>
        try {
          Reader.Success(from.toInt to to.toInt)
        } catch {
          case _: Exception => Reader.Error(s"$str must be a numeric range")
        }
      case _ => Reader.Error(s"expected 'from..to', found: $str")
    }
    def typeName = "from..to"
  }
}


trait LowPrioReaders { self: TypesApi =>
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
    def typeName = s"list of ${elementReader.typeName}s separated by ','"
  }
}
