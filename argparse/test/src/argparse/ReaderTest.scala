package argparse

import utest._

object ReaderTest extends TestSuite {

  val tests = Tests {
    test("int") {
      val parser = new TestParser
      parser.requiredParam[Int]("p1")
      parser.parseResult(Seq("1"))
      parser.parseErrors ==> 0
    }
    test("int is not float") {
      val parser = new TestParser
      parser.requiredParam[Int]("p1")
      parser.parseResult(Seq("1.0"))
      parser.parseErrors ==> 1
    }
    test("float") {
      val parser = new TestParser
      parser.requiredParam[Double]("p1")
      parser.parseResult(Seq("1.0"))
      parser.parseErrors ==> 0
    }
    test("float is int") {
      val parser = new TestParser
      parser.requiredParam[Double]("p1")
      parser.parseResult(Seq("1"))
      parser.parseErrors ==> 0
    }
    test("option") {
      val parser = new TestParser
      val p = parser.requiredParam[Option[Int]]("p1")
      parser.parseResult(Seq("1"))
      parser.parseErrors ==> 0
      p() ==> Some(1)
    }
    test("missing option") {
      val parser = new TestParser
      val p = parser.requiredParam[Option[Int]]("p1")
      parser.parseResult(Seq())
      parser.parseErrors ==> 0
      // the type does not affect the parser; a missing param is still a missing param
      parser.missing ==> 1
    }
    test("collection") {
      val parser = new TestParser
      val p = parser.requiredParam[Seq[String]]("-p")
      parser.parseResult(Seq("-p", "a,b,c", "-p", "c,d,e")) ==> ArgParser.Success
      p() ==> Seq("c", "d", "e")
    }
    test("repeated collection") {
      val parser = new TestParser
      val p = parser.repeatedParam[Seq[String]]("-p")
      parser.parseResult(Seq("-p", "a,b,c", "-p", "c,d,e")) ==> ArgParser.Success
      p() ==> List(List("a", "b", "c"), List("c", "d", "e"))
    }
    test("key-value") {
      val parser = new TestParser
      val p = parser.repeatedParam[(String, Int)]("-p")
      parser.parseResult(Seq("-p=a=1", "-p", "c=2")) ==> ArgParser.Success
      p() ==> List("a" -> 1, "c" -> 2)
    }
    test("input stream") {
      val parser = new TestParser
      val param = parser.requiredParam[() => java.io.InputStream]("p1")
      parser.parseResult(Seq("build.sc"))
      parser.parseErrors ==> 0
      val expected = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get("build.sc")))
      val stream = param()()
      try {
        scala.io.Source.fromInputStream(stream).mkString ==> expected
      } finally stream.close()
    }
    test("readable") {
      val parser = new TestParser
      val param = parser.requiredParam[geny.Readable]("p1")
      parser.parseResult(Seq("build.sc"))
      parser.parseErrors ==> 0
      val expected = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get("build.sc")))
      param().readBytesThrough{ stream =>
        scala.io.Source.fromInputStream(stream).mkString ==> expected
      }
    }
    test("output stream") {
      val parser = new TestParser
      val param = parser.requiredParam[() => java.io.OutputStream]("p1")
      parser.parseResult(Seq("-"))
      parser.parseErrors ==> 0

      val testData = "hello, world"

      val bytes = new java.io.ByteArrayOutputStream()
      val byteStream = new java.io.PrintStream(bytes)

      val savedOut = System.out
      try {
        System.setOut(byteStream)
        val stream = param()()
        try {
          stream.write(testData.getBytes())
          stream.flush()
        } finally {
          stream.close()
        }
      } finally {
        System.setOut(savedOut)
        byteStream.close()
      }

      bytes.toByteArray().toList ==> testData.getBytes().toList
    }
    test("filepathseq") {
      val parser = new TestParser
      val param = parser.requiredParam[Iterable[os.FilePath]]("p1")
      parser.parseResult(Seq("/a:./b:../c:d"))
      parser.parseErrors ==> 0
      param() ==> Seq(os.FilePath("/a"), os.FilePath("./b"), os.FilePath("../c"), os.FilePath("d"))
    }
    test("pathseq") {
      val parser = new TestParser
      val param = parser.requiredParam[Seq[os.Path]]("p1")
      parser.parseResult(Seq("/a:./b:../c:d"))
      parser.parseErrors ==> 0
      //param().map(_.isInstanceOf[os.Path]).foreach(println)
      param() ==> Seq(os.Path("/a"), os.pwd / "b", os.pwd / os.up / "c", os.pwd / "d")
    }
    test("subpathseq") {
      val parser = new TestParser
      val param = parser.requiredParam[Seq[os.SubPath]]("p1")

      test("invalid") {
        parser.parseResult(Seq("/a:./b:../c:d"))
        parser.parseErrors ==> 1
      }
      test("invalid2") {
        parser.parseResult(Seq("a/../../b"))
        parser.parseErrors ==> 1
      }
      test("valid") {
        parser.parseResult(Seq("a:./a:a/../b"))
        parser.parseErrors ==> 0
        param() ==> Seq(os.SubPath("a"), os.SubPath("a"), os.SubPath("b"))
      }
    }
  }

}
