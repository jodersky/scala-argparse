import utest._

object Negtest extends DynamicTestSuite {
  import argparse.ini

  testAll("neg"){ inFile =>
    val outFile = inFile / os.up / (inFile.baseName + ".txt")

    val parser = new ini.ConfigParser()
    val s = os.read.inputStream(inFile)
    try {
      val err = intercept[ini.ParseException] {
        parser.parse(s, inFile.baseName.toString)
      }
      DiffTools.assertNoDiff(outFile, err.pretty())
    } finally {
      s.close()
    }
  }

}
