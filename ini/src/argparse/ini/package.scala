package argparse

package object ini {

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
  def read(inputs: Parseable*): Obj = {
    val parser = new ConfigParser()
    for (input <- inputs.reverse) {
      parser.parse(input.readable, input.filename)
    }
    parser.rootSection
  }

}
