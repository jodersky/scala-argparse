package argparse
import argparse.ini.Section
import argparse.ini.Str

object Ini extends (os.Path => Either[String, String => Option[ArgParser.ConfigValue]]) {

  def apply(path: os.Path) = {
    val parser = ini.ConfigParser()
    try {
      parser.parse(os.read.stream(path), path.toString())
      val section = ini.Section(parser.root)

      val fct = (key: String) => {
        val parts = key.split('.').toList

        var sec = section
        for (seg <- parts.init) {
          sec.value(seg) match {
            case s: Section =>
              sec = s
            case Str(value) => // TODO: error
          }
        }

        sec.value.get(parts.last) match {
          case None => None
          case Some(Section(_)) => None
          case Some(s: Str) =>
            Some(ArgParser.ConfigValue(s.pos.file, s.pos.line, s.pos.col, s.value))
        }
      }
      Right(fct)
    } catch {
      case ini.ParseException(pos, message, _) => Left(message)
    }
  }

}

// case class DummyMap(data: Map[String, String]) extends (os.Path => Either[String, String => Option[ArgParser.ConfigValue]]) {
//   def apply(path: os.Path) = Right(key => data.get(key))
// }
