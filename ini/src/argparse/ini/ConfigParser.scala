package argparse.ini

import scala.collection.mutable.LinkedHashMap

/** INI-syle config parser. */
class ConfigParser() {

  val root: LinkedHashMap[String, Value] = LinkedHashMap.empty
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
    throw new ParseException(pos, message, line)
  }

  private def prettyChar(char: Int) = {
    if (char == -1) "EOF"
    else char.toChar match {
      case '\n' => "new line"
      case c => s"'$c'"
    }
  }

  private def expectationError(expected: String*) = {
    errorAt(cpos, s"Expected ${expected.mkString(" or ")}. Found ${prettyChar(char)}.")
  }

  private def skipSpace() = {
    while (char == ' ' || char == '\t') {
      readChar()
    }
  }

  // utility text buffer
  private val buffer = new StringBuilder

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

  private def parseKey(): List[String] = {
    val segs = collection.mutable.ListBuffer.empty[String]
    segs += parseSegment()
    while (char == '.') {
      readChar()
      segs += parseSegment()
    }
    segs.toList
  }

  private def makeSection(pos: Position, path: List[String]): Obj = {
    var section = currentSection
    val it = path.iterator
    while (it.hasNext) {
      val segment = it.next()
      section.get(segment) match {
        case None =>
          val s = Obj().setPos(pos)
          section += segment -> s
          section = s.value
        case Some(s: Obj) =>
          section = s.value
        case Some(other) =>
          errorAt(
            pos,
            s"Cannot create a section that is already defined as a key (previous definition at ${other.pos})"
          )
      }
    }
    Obj(section)
  }

  private def parseSection() = {
    val pos = cpos

    if (char != '[') expectationError("'['")
    readChar()
    skipSpace()
    val sectionPath = parseKey()
    currentSection = root
    currentSection = makeSection(pos, sectionPath).value
    skipSpace()
    if (char != ']')  expectationError("']'")
    readChar()
  }

  private def parseKeyValue() = {
    val pos = cpos
    val key = parseKey()
    val section = makeSection(pos, key.init)

    skipSpace()
    if (char != '=') expectationError("'='")
    readChar()
    skipSpace()

    // parse rhs as plain text until new line
    buffer.clear()
    while (char != -1 && char != '\n') {
      buffer += char.toChar
      readChar()
    }
    val value = buffer.result()

    section.value.get(key.last) match {
      case Some(other: Obj) => errorAt(
        pos,
        s"Cannot create a key that is already defined as a section (previous definition at ${other.pos})"
      )
      case _ =>
        section.value(key.last) = Str(value).setPos(pos)
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

  val rootSection = Obj(root)

  private var firstPass = true

  // This method can be called multiple times. Configuration value  will be
  // overridden as encountered.
  def parse(input: java.io.InputStream, filename: String): Unit = {
    this.input = input
    this.filename = filename
    currentSection = root
    cline = 1
    ccol = 1
    lineBuffer.clear()

    if (firstPass) {
      firstPass = false
      rootSection.setPos(cpos)
    }

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
) extends Exception(message: String) {
  def pretty() = {
    val caret = " " * (position.col - 1) + "^"
    s"$message\n$position\n$line\n$caret"
  }

}
