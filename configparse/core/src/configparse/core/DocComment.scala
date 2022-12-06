package configparse.core

case class DocComment(paragraphs: Iterable[String], params: collection.Map[String, String])

object DocComment:

  private val Param = """@param\s+(\w+)\s+(.*)""".r

  def extract(comment: String): DocComment =
    val content = comment.drop(3).dropRight(2) // all doc comments start with /** and end with */
    val lines = content.linesIterator

    val paragraphs = collection.mutable.ListBuffer.empty[String]
    val params = collection.mutable.LinkedHashMap.empty[String, String]

    val paragraph = StringBuilder()
    var prevIsBlank = true

    var line: String = ""
    var eof = false

    def readLine() =
      eof = !lines.hasNext
      if !eof then
        line = lines.next().dropWhile(_ == ' ').dropWhile(_ == '*').trim

    readLine()
    while !eof do
      while !eof && line.isEmpty() do readLine()

      if !eof && !line.isEmpty() then
        paragraph ++= line
        readLine()
        while !eof && !line.isEmpty() do
          paragraph += ' '
          paragraph ++= line
          readLine()
        end while

        paragraph.result() match
          case Param(name, desc) => params += name -> desc
          case regular => paragraphs += regular
        paragraph.clear()
      end if
    end while

    DocComment(paragraphs, params)
  end extract


  import quoted.Expr, quoted.Varargs, quoted.Type, quoted.Quotes

  inline given DocComment = ${here}

  def here(using qctx: Quotes): Expr[DocComment] =
    import qctx.reflect.*
    val s = Symbol.spliceOwner.owner.docstring.getOrElse("")
    '{extract(${Expr(s)})}
