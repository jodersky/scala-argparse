package argparse.core

case class DocComment(paragraphs: Iterable[String], params: collection.Map[String, String])

object DocComment:

  private val Param = """@param\s+(\w+)\s*(.*)""".r

  def extract(comment: String): DocComment =
    val content = comment.drop(3).dropRight(2) // all doc comments start with /** and end with */
    val lines = content.linesIterator

    val paragraphs = collection.mutable.ListBuffer.empty[String]
    val paragraph = StringBuilder()

    var line: String = ""
    var eof = false

    def readLine() =
      eof = !lines.hasNext
      if !eof then
        line = lines.next().dropWhile(_ == ' ').dropWhile(_ == '*').trim

    def readParagraph() =
      paragraph.clear()
      if !eof && !line.isEmpty() then
        paragraph ++= line
        readLine()
        while !eof && !line.isEmpty() && !line.startsWith("@") do
          paragraph += ' '
          paragraph ++= line
          readLine()
        end while

    def readParagraphs() =
      paragraphs.clear()
      while !eof && !line.startsWith("@") do
        while !eof && line.isEmpty() do readLine() // skip blanks
        if !eof && !line.startsWith("@") then
          readParagraph()
          paragraphs += paragraph.result()

    // readLine()
    var mainDoc: List[String] = Nil
    if !eof then
      readParagraphs()
      mainDoc = paragraphs.result()

    val params = collection.mutable.LinkedHashMap.empty[String, String]
    while !eof do
      line match
        case Param(name, rest) =>
          line = rest
          readParagraphs()
          params += name -> paragraphs.result().mkString(" ")
        case _ => readParagraph()
    end while

    DocComment(mainDoc, params)
  end extract


  import quoted.Expr, quoted.Varargs, quoted.Type, quoted.Quotes

  inline given DocComment = ${here}

  def here(using qctx: Quotes): Expr[DocComment] =
    import qctx.reflect.*
    val s = Symbol.spliceOwner.owner.docstring.getOrElse("")
    '{extract(${Expr(s)})}
