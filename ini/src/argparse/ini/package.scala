package argparse

package object ini {

  case class Parseable(readable: geny.Readable, filename: String)
  object Parseable {
    import scala.language.implicitConversions

    implicit def FileParseable(path: os.Path): Parseable = Parseable(os.read.stream(path), path.toString)
    //implicit def ReadableParseable(readable: geny.Readable): Parseable = Parseable(readable, "<virtual>")
    implicit def ReadableParseable[T](t: T)(implicit mkReadable: T => geny.Readable): Parseable = Parseable(mkReadable(t), "<virtual>")

    implicit def Tuple[T](named: (T, String))(implicit f: T => Parseable): Parseable = Parseable(f(named._1).readable, named._2)
  }

  /** Priority-ordered input files. When config values will be
    * looked up, the entry first encountered in this list will be used.
    */
  def read[A](inputs: A*)(implicit f: A => Parseable): Obj = {
    val parser = new ConfigParser()
    for (input0 <- inputs.reverse) {
      val input = f(input0)
      parser.parse(input.readable, input.filename)
    }
    parser.rootSection
  }

  def write(value: Value, indentation: Int = 2): String = {
    val baos = new java.io.ByteArrayOutputStream()
    val printer = new FlatPrinter(baos, indentation)
    printer.print(value)
    baos.toString("utf-8")
  }

}
