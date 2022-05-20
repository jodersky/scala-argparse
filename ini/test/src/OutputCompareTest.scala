import utest._

object OutputCompareTest extends DynamicTestSuite {
  import argparse.ini

  def jsonify(section: ini.Obj): ujson.Obj = {
    val jsroot = ujson.Obj().obj
    section.value.foreach{
      case (key, value: ini.Obj) =>
        jsroot += key -> jsonify((value))
      case (key, value: ini.Str) =>
        jsroot += key -> ujson.Str(value.value)
    }
    jsroot
  }

  testAll("checks") { inFile =>
    val outFile = inFile / os.up / (inFile.baseName + ".json")

    val parser = new ini.ConfigParser()
    val s = os.read.inputStream(inFile)
    try {
      parser.parse(s, inFile.toString)
    } finally {
      s.close()
    }

    val json = jsonify(parser.rootSection).render(2)
    val cleaned = json.linesIterator.map(_.reverse.dropWhile(_.isSpaceChar).reverse)
    DiffTools.assertNoDiff(outFile, cleaned.mkString("", "\n", "\n"))
  }

  testAll("precedence", filter = p => os.isDir(p)) { dir =>
    val outFile = dir / os.up / (dir.baseName + ".json")

    val section = ini.read(os.list(dir).sorted: _*)

    val json = jsonify(section).render(2)
    val cleaned = json.linesIterator.map(_.reverse.dropWhile(_.isSpaceChar).reverse)
    DiffTools.assertNoDiff(outFile, cleaned.mkString("", "\n", "\n"))
  }

}
