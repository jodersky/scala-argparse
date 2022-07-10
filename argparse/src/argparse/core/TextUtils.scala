package argparse.core

object TextUtils {
  def wrap(in: String, out: StringBuilder, width: Int, newLine: String): Unit = {
    if (in.length < width) {
      out ++= in
      return
    }

    var offset = 0
    var segStart = 0
    var wordStart = 0
    var col = 0

    while (offset < in.length) {
      segStart = offset
      while(offset < in.length && in.charAt(offset).isWhitespace) {
        offset += 1
        col += 1
      }
      wordStart = offset
      while(offset < in.length && !in.charAt(offset).isWhitespace) {
        offset += 1
        col += 1
      }

      if (col >= width) {
        out ++= newLine
        out ++= in.substring(wordStart, offset)
        col = offset - wordStart
      } else {
        out ++= in.substring(segStart, offset)
      }
    }
  }

  /** `thisIsKebabCase => this-is-kebab-case` */
  def kebabify(camelCase: String): String = {
    val kebab = new StringBuilder
    var prevIsLower = false
    for (c <- camelCase) {
      if (prevIsLower && c.isUpper) {
        kebab += '-'
      }
      kebab += c.toLower
      prevIsLower = c.isLower
    }
    kebab.result()
  }
}
