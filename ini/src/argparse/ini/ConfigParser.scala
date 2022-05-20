package argparse.ini

import scala.collection.mutable.LinkedHashMap

/** INI-syle config parser. */
class ConfigParser(
  val root: LinkedHashMap[String, Value] = LinkedHashMap.empty,
) {

  private var input: java.io.InputStream = null
  private var filename: String = "none"
  private var currentSection = root
  private var cline = 1
  private var ccol = 1
  private def cpos = Position(filename, cline, ccol)

  private var char: Int = -1

  // up to the current character (not included)
  private val lineBuffer = new StringBuilder()

  @inline private def readChar(): Unit = {
    char match {
      case '\n' =>
        ccol = 1
        cline += 1
        lineBuffer.clear()
      case -1 | '\r' => // invisible chars, do nothing
      case _ =>
        ccol += 1
        lineBuffer += char.toChar
    }
    char = input.read()
  }

  private def errorAt(pos: Position, message: String): Nothing = {
    // read until end of line
    while (char != -1 && char != '\n') {
      readChar()
    }

    val line = lineBuffer.result()
    val caret = " " * (pos.col - 1) + "^"
    val pretty = s"$message\n$pos\n$line\n$caret"

    throw new ParseException(pos, pretty, line)
  }

  private def prettyChar(char: Int) = char match {
    case -1 => "EOF"
    case c => c.toChar
  }

  private def expectationError(expected: String*) = {
    errorAt(cpos, s"Expected ${expected.mkString(" or ")}. Found ${prettyChar(char)}.")
  }

  private def skipSpace() = {
    while (char == ' ' || char == '\t') {
      readChar()
    }
  }

  // segment buffer
  private val buffer = new StringBuilder
  private def parseText(): String = {
    buffer.clear()
    while (char != -1 && char != '\n') {
      buffer += char.toChar
      readChar()
    }
    buffer.result()
  }

  private def parseSegment(): String = {
    import java.lang.Character

    if (Character.isAlphabetic(char) || Character.isDigit(char) || char == '_' || char == '-') {
      buffer.clear()
      while (Character.isAlphabetic(char) || Character.isDigit(char) || char == '_' || char == '-') {
        buffer += char.toChar
        readChar()
      }
      buffer.result()
    } else {
      expectationError("alphanumeric", "'_'", "'-'")
    }
  }

  private def parseSectionHeading(): List[String] = {
    val segs = collection.mutable.ListBuffer.empty[String]
    segs += parseSegment()
    while (char == '.') {
      readChar()
      segs += parseSegment()
    }
    segs.toList
  }

  private def parseSection() = {
    val pos = cpos

    if (char != '[') expectationError("'['")
    readChar()
    skipSpace()
    val sectionPath = parseSectionHeading()
    skipSpace()
    if (char != ']')  expectationError("']'")
    readChar()

    currentSection = root
    for (seg <- sectionPath) {
      currentSection.get(seg) match {
        case None =>
          val s = Section().setPos(pos)
          currentSection += seg -> s
          currentSection = s.value
        case Some(s: Section) =>
          currentSection = s.value
        case Some(other) =>
          errorAt(
            pos,
            "Attempting to define a section of the same name as a key already defined at: " + other.pos
          )
      }
    }
  }

  private def parseKeyValue() = {
    val pos = cpos
    val key = parseSegment()
    skipSpace()
    if (char != '=') errorAt(cpos, s"Expected '='. Found: '${prettyChar(char)}'")
    readChar()
    skipSpace()
    val value = parseText()

    currentSection.get(key) match {
      case Some(other: Section) => errorAt(
        cpos,
        "Attempting to define a key of the same name as a section already defined at: " + other.pos
      )
      case _ => currentSection(key) = Str(value).setPos(pos)
    }
  }

  private def parseNext() = {
    while (char == ' ' || char == '\t' || char == '\n') {
      readChar()
    }
    char match {
      case -1 =>
      case '[' => parseSection()
      case ';' | '#' =>
        while (char != -1 && char != '\n') {
          readChar()
        }
      case s => parseKeyValue()
    }
  }


  // This method can be called multiple times. Configuration value  will be
  // overridden as encountered.
  def parse(input: java.io.InputStream, filename: String): Unit = {
    this.input = input
    this.filename = filename
    currentSection = root
    cline = 1
    ccol = 1
    lineBuffer.clear()
    char = input.read()
    while (char != -1) {
      parseNext()
    }
  }

  def parse(readable: geny.Readable, filename: String): Unit = {
    readable.readBytesThrough(parse(_, filename))
  }

}

case class ParseException(
  position: Position,
  message: String,
  line: String
) extends Exception(message: String)
