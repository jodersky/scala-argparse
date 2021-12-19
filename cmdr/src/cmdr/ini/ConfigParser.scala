package cmdr.ini

import scala.collection.mutable.LinkedHashMap

/** INI-syle config parser. */
case class ConfigParser() {

  private var input: java.io.InputStream = null
  private var filename: String = "none"
  private var cline = 1
  private var ccol = 0
  private def cpos = Position(filename, cline, ccol)

  private var char: Int = -1

  private val lineBuffer = new StringBuilder()
  @inline private def readChar(): Unit = {
    char = input.read()
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
  }

  private def errorHere(message: String): ParseException = {
    val pos = cpos
    // read until end of line
    while (!(char == -1 || char == '\n')) {
      readChar()
    }

    val line = lineBuffer.result()
    val caret = " " * (pos.col - 1) + "^"
    val pretty = s"$message\n$pos\n$line\n$caret"

    new ParseException(pos, pretty, line)
  }

  private def prettyChar(char: Int) = char match {
    case -1 => "EOF"
    case c => c.toChar
  }

  private def skipSpace() = {
    while (char == ' ' || char == '\t') {
      readChar()
    }
  }

  private val buffer = new StringBuilder
  private def parseText(): String = {
    buffer.clear()
    while (!(char == -1 || char == '\n')) {
      buffer += char.toChar
      readChar()
    }
    buffer.result()
  }

  private def parseSegment(): String = {
    if (java.lang.Character.isAlphabetic(char) || java.lang.Character.isDigit(char) || char == '_' || char == '-') {
      buffer.clear()
      while (java.lang.Character.isAlphabetic(char) || java.lang.Character.isDigit(char) || char == '_' || char == '-') {
        buffer += char.toChar
        readChar()
      }
      buffer.result()
    } else {
      throw errorHere(s"Expected start of key. Found: '${prettyChar(char)}'")
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

    if (char != '[') throw errorHere(s"Expected '['. Found: '${prettyChar(char)}'")
    readChar()
    skipSpace()
    val sectionPath = parseSectionHeading()
    skipSpace()
    if (char != ']') throw errorHere(s"Expected ']'. Found: '${prettyChar(char)}'")
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
          throw errorHere(
            "Attempting to define a section of the same name as a key already defined at: " + other.pos
          )
      }
    }
  }

  private def parseKeyValue() = {
    val pos = cpos
    val key = parseSegment()
    skipSpace()
    if (char != '=') throw errorHere(s"Expected '='. Found: '${prettyChar(char)}'")
    readChar()
    skipSpace()
    val value = parseText()

    currentSection.get(key) match {
      case Some(other: Section) => throw errorHere(
        "Attempting to define a key of the same name as a section already defined at: " + other.pos
      )
      case _ => currentSection(key) = Str(value).setPos(pos)
    }
  }

  val root = LinkedHashMap.empty[String, Value]
  private var currentSection = root

  private def parseNext() = {
    while (char == ' ' || char == '\t' || char == '\n') {
      readChar()
    }
    char match {
      case -1 =>
      case '[' => parseSection()
      case ';' | '#' =>
        while (!(char == -1 || char == '\n')) {
          readChar()
        }
      case s => parseKeyValue()
    }
  }

  def parse(input: java.io.InputStream, filename: String): Unit = {
    this.input = input
    this.filename = filename
    currentSection = root
    cline = 1
    ccol = 0
    lineBuffer.clear()
    readChar()
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
