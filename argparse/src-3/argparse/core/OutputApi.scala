package argparse.core

import javax.swing.GroupLayout.Alignment

trait OutputApi extends ParsersApi with TypesApi with Printers:

  /** Top-level error handler for command line applications using the annotation
    * API.
    *
    * You can override this to change what should be done on errors.
    *
    * The default implementation prints the exception's message unless the DEBUG
    * environment variable is set, in which case the whole stack trace is
    * printed. Then, it exits with error code 1.
    */
  def handleError(t: Throwable): Nothing =
    if sys.env.contains("DEBUG") then
      t.printStackTrace()
    else
      System.err.println(t.getMessage())
    exit(1)

  trait Printer[A]:
    def print(
      a: A,
      out: java.io.PrintStream,
      info: OutputApi.StreamInfo
    ): Unit

object OutputApi:

  case class StreamInfo(
    isatty: Boolean
  )

  enum Alignment:
    case Left
    case Right
    case Numeric

  def tabulate(
    rows: Iterable[Iterable[_]],
    header: Iterable[String] = Iterable.empty,
    alignments: Iterable[Alignment] = Iterable.empty
  ): String = {
    if (header.isEmpty && rows.isEmpty) return ""

    val ncols = if (!header.isEmpty) {
      header.size
    } else {
      rows.head.size
    }

    val aligns = if (alignments.isEmpty) {
      if (!rows.isEmpty) {
        rows.head.map{ elem =>
          val s = elem.toString
          if (s.size > 0 && (s(0) == '-' || s(0).isDigit)) {
            Alignment.Numeric
          } else {
            Alignment.Left
          }
        }.toArray
      } else {
        header.map(_ => Alignment.Left).toArray
      }
    } else {
      alignments.toArray
    }
    var i = 0
    var j = 0

    val leftMosts = Array.fill(ncols)(0)
    val rightMosts = Array.fill(ncols)(0)

    val strings = for (row <- rows) yield {
      i = 0
      for (elem <- row) yield {
        val s = elem.toString

        if (aligns(i) == Alignment.Numeric) {
          j = 0
          var commaPos = s.length
          while (j < s.length && commaPos == s.length) {
            if (s.charAt(j) == '.') commaPos = j
            j += 1
          }

          if (commaPos > leftMosts(i)) leftMosts(i) = commaPos
          if ((s.length - commaPos) > rightMosts(i)) rightMosts(i) = s.length - commaPos
        } else {
          if (s.length > leftMosts(i)) leftMosts(i) = s.length
        }
        i += 1
        s
      }
    }

    val maxWidths = Array.fill(ncols)(0)

    val header1 = header.toArray

    i = 0
    while (i < leftMosts.size) {
      val w = leftMosts(i) + rightMosts(i)
      if (w > maxWidths(i)) maxWidths(i) = w
      if (!header.isEmpty && header1(i).size > maxWidths(i)) maxWidths(i) = header1(i).size
      i += 1
    }

    val buffer = new collection.mutable.StringBuilder
    val srows = strings.iterator

    inline def printElemLeft(elem: String) = {
      buffer ++= elem
      val padding = maxWidths(i) - elem.length
      j = 0
      while (j < padding) {
        buffer += ' '
        j += 1
      }
    }

    inline def printElemRight(elem: String) = {
      val padding = maxWidths(i) - elem.length
      j = 0
      while (j < padding) {
        buffer += ' '
        j += 1
      }
      buffer ++= elem
    }

    inline def printElemNumeric(elem: String) = {
      j = 0
      var commaPos = elem.length
      while (j < elem.size && commaPos == elem.length) {
        if (elem.charAt(j) == '.') commaPos = j
        j += 1
      }

      val paddingL = (maxWidths(i) - leftMosts(i) - rightMosts(i)) + leftMosts(i) - commaPos
      j = 0
      while (j < paddingL) {
        buffer += ' '
        j += 1
      }
      buffer ++= elem
      val paddingR = rightMosts(i) - (elem.length - commaPos)
      j = 0
      while (j < paddingR) {
        buffer += ' '
        j += 1
      }
    }

    inline def printElem(elem: String) = aligns(i) match {
      case Alignment.Left => printElemLeft(elem)
      case Alignment.Right => printElemRight(elem)
      case Alignment.Numeric => printElemNumeric(elem)
    }

    inline def printRow(elems: Iterator[String]) = {
      i = 0
      if (elems.hasNext) {
        printElem(elems.next)
        i += 1
      }
      while (elems.hasNext) {
        buffer += ' '
        printElem(elems.next)
        i += 1
      }
    }

    if (!header.isEmpty) {
      val elems = header.iterator // if header is not empty, the first srows elem is the header
      i = 0
      if (elems.hasNext) {
        printElemLeft(elems.next)
        i += 1
      }
      while (elems.hasNext) {
        buffer += ' '
        printElemLeft(elems.next)
        i += 1
      }
    }

    if (srows.hasNext) {
      if (!header.isEmpty) buffer += '\n'
      printRow(srows.next().iterator)
    }
    while (srows.hasNext) {
      buffer += '\n'
      printRow(srows.next().iterator)
    }
    buffer.toString
  }

  def tabulatep(
    rows: Iterable[Product],
    headers: Iterable[String] = null,
    alignments: Iterable[Alignment] = Iterable.empty
  ): String =
    tabulate(
      rows.map(_.productIterator.toIterable),
      if headers == null then
        if !rows.isEmpty then rows.head.productElementNames.toIterable.map(_.toUpperCase())
        else Iterable.empty
      else headers,
      alignments
    )

trait Printers extends LowPrioPrinters:
  self: OutputApi =>

  given Printer[Unit] with
    def print(a: Unit, out: java.io.PrintStream, info: OutputApi.StreamInfo): Unit = ()

  given Printer[Array[Byte]] with
    def print(a: Array[Byte], out: java.io.PrintStream, info: OutputApi.StreamInfo): Unit =
      out.write(a)

  given Printer[geny.Writable] with
    def print(a: geny.Writable, out: java.io.PrintStream, info: OutputApi.StreamInfo): Unit =
      a.writeBytesTo(out)

  given [A](using p: Printer[A]): Printer[geny.Generator[A]] with
    def print(value: geny.Generator[A], out: java.io.PrintStream, info: OutputApi.StreamInfo): Unit =
      value.foreach(v => p.print(v, out, info))

  inline given productListPrinter[A <: Iterable[B], B <: Product](using m: ProductLabels[B]): Printer[A] with
    def print(value: A, out: java.io.PrintStream, info: OutputApi.StreamInfo): Unit =
      val rows = value.map(_.productIterator.toIterable)
      if info.isatty then
        out.println(OutputApi.tabulate(rows, header = m.labels.map(_.toUpperCase)))
      else
        out.println(OutputApi.tabulate(rows, header = Iterable.empty))

  given nonProductListPrinter[A <: Iterable[B], B](using elemPrinter: Printer[B]): Printer[A] with
    def print(value: A, out: java.io.PrintStream, info: OutputApi.StreamInfo): Unit =
      for elem <- value do elemPrinter.print(elem, out, info)

  given [A](using p: Printer[A]): Printer[concurrent.Future[A]] with
    def print(value: concurrent.Future[A], out: java.io.PrintStream, info: OutputApi.StreamInfo): Unit =
      p.print(
        concurrent.Await.result(value, concurrent.duration.Duration.Inf),
        out,
        info
      )

trait LowPrioPrinters:
  self: OutputApi =>

  // fallback to always print something
  given [A]: Printer[A] with
    def print(a: A, out: java.io.PrintStream, info: OutputApi.StreamInfo): Unit =
      out.println(a.toString)
