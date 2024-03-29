import utest._

object ArgParserTest extends TestSuite {

  val tests = Tests {
    test("empty") {
      val parser = new TestParser
      parser.parseResult(Nil)

      parser.missing ==> 0
      parser.unknown ==> 0
      parser.parseErrors ==> 0
    }
    test("positional") {
      val parser = new TestParser
      parser.requiredParam[String]("p1")
      parser.requiredParam[String]("p2")
      parser.parseResult(Nil)

      parser.missing ==> 2
      parser.unknown ==> 0
    }
    test("positional with defaults") {
      val parser = new TestParser
      parser.param[String]("p1", "a")
      parser.param[String]("p2", "b")
      parser.parseResult(Nil)

      parser.missing ==> 0
      parser.unknown ==> 0
    }
    test("positional partial") {
      val parser = new TestParser
      parser.requiredParam[String]("p1")
      parser.requiredParam[String]("p2")
      parser.parseResult("foo" :: Nil)

      parser.missing ==> 1
      parser.unknown ==> 0
    }
    test("named args") {
      val parser = new TestParser
      parser.requiredParam[String]("--p1")
      parser.requiredParam[String]("--p2")
      parser.parseResult("--p1=a" :: "--p2=b" :: Nil)

      parser.missing ==> 0
      parser.unknown ==> 0
    }
    test("named args missing") {
      val parser = new TestParser
      parser.requiredParam[String]("--p1")
      parser.requiredParam[String]("--p2")
      parser.parseResult(Nil)

      parser.missing ==> 2
      parser.unknown ==> 0
    }
    test("named args missing default") {
      val parser = new TestParser
      parser.param[String]("--p1", "a")
      parser.requiredParam[String]("--p2")
      parser.parseResult(Nil)

      parser.missing ==> 1
      parser.unknown ==> 0
    }
    test("named args too many") {
      val parser = new TestParser
      parser.requiredParam[String]("--p1")
      parser.requiredParam[String]("--p2")
      parser.parseResult("--p2=a" :: "--p3=c" :: Nil)

      parser.missing ==> 1
      parser.unknown ==> 1
    }
    test("named and positional") {
      val parser = new TestParser
      val n1 = parser.requiredParam[String]("--n1")
      val n2 = parser.requiredParam[String]("--n2")
      val p1 = parser.requiredParam[String]("p1")
      val p2 = parser.requiredParam[String]("p2")
      val n3 = parser.requiredParam[String]("--n3")
      parser.parseResult("--n2=a" :: "b" :: "--n3=c" :: "d" :: "--n1=e" :: Nil)

      n1.value ==> "e"
      n2.value ==> "a"
      n3.value ==> "c"
      p1.value ==> "b"
      p2.value ==> "d"
      parser.missing ==> 0
      parser.unknown ==> 0
    }
    test("escaping") {
      val parser = new TestParser
      val n1 = parser.requiredParam[String]("--n1")
      val p1 = parser.requiredParam[String]("p1")
      val p2 = parser.requiredParam[String]("p2")
      parser.parseResult("a" :: "--" :: "--n3=c" :: Nil)

      p1.value ==> "a"
      p2.value ==> "--n3=c"
      parser.missing ==> 1
      parser.unknown ==> 0
    }
    test("typed int") {
      val parser = new TestParser
      val n1 = parser.param("--n1", 2)
      parser.parseResult("--n1=2" :: Nil)

      parser.missing ==> 0
      parser.unknown ==> 0
      parser.parseErrors ==> 0
      n1.value ==> 2
    }
    test("typed error") {
      val parser = new TestParser
      val n1 = parser.param[Int]("--n1", 2)
      parser.parseResult("--n1=2.2" :: Nil)

      parser.missing ==> 0
      parser.unknown ==> 0
      parser.parseErrors ==> 1
    }
    test("typed list") {
      val parser = new TestParser
      val n1 = parser.requiredParam[List[Int]]("--n1")
      parser.parseResult("--n1=2,3" :: Nil)

      parser.missing ==> 0
      parser.unknown ==> 0
      parser.parseErrors ==> 0
      n1.value ==> List(2, 3)
    }
    test("repeated named") {
      val parser = new TestParser
      val n1 = parser.repeatedParam[String]("--n1")
      parser.parseResult("--n1=a" :: "--n1=b" :: "--n1=c" :: Nil)

      parser.missing ==> 0
      parser.unknown ==> 0
      parser.parseErrors ==> 0
      n1.value ==> List("a", "b", "c")
    }
    test("repeated positional") {
      val parser = new TestParser
      val n1 = parser.repeatedParam[String]("--n1")
      val p1 = parser.requiredParam[String]("p1")
      val r1 = parser.repeatedParam[String]("r1")
      parser.parseResult("a" :: "b" :: "--n1=c" :: "d" :: "e" :: Nil)

      parser.missing ==> 0
      parser.unknown ==> 0
      parser.parseErrors ==> 0
      n1.value ==> List("c")
      p1.value ==> "a"
      r1.value ==> List("b", "d", "e")
    }
    test("repeated mix") {
      val parser = new TestParser
      val n1 = parser.repeatedParam[String]("--n1")
      val n2 = parser.param[String]("--n2", "none") // not a repeated param, last value overrides
      val p1 = parser.requiredParam[String]("p1")
      val r1 = parser.repeatedParam[String]("r1")
      parser.parseResult(
        List(
          "--n1=a",
          "b",
          "c",
          "--n2=d",
          "--n1=e",
          "--n2=f",
          "g",
          "h",
          "--",
          "--n2=i"
        )
      )

      parser.missing ==> 0
      parser.unknown ==> 0
      parser.parseErrors ==> 0
      n1.value ==> List("a", "e")
      n2.value ==> "f"
      p1.value ==> "b"
      r1.value ==> List("c", "g", "h", "--n2=i")
    }
    test("missing argument's argument") {
      val parser = new TestParser
      val n1 = parser.requiredParam[String]("--n1")
      parser.parseResult("--n1" :: Nil)

      parser.missing ==> 0
      parser.unknown ==> 0
      parser.parseErrors ==> 1
    }
    test("flag") {
      val parser = new TestParser
      val n1 = parser.requiredParam[String]("--n1", flag = true)
      parser.parseResult("--n1" :: Nil)

      parser.missing ==> 0
      parser.unknown ==> 0
      parser.parseErrors ==> 0
      n1.value ==> "true"
    }
    test("flag override with embedded") {
      val parser = new TestParser
      val n1 = parser.requiredParam[String]("--n1", flag = true)
      parser.parseResult("--n1=yesss!" :: Nil)

      parser.missing ==> 0
      parser.unknown ==> 0
      parser.parseErrors ==> 0
      n1.value ==> "yesss!"
    }
    test("flag absent without default value") {
      val parser = new TestParser
      val n1 = parser.requiredParam[String]("--n1", flag = true) // a flag without default value doesn't make much sense but is possible
      parser.parseResult(Nil)

      parser.missing ==> 1
      parser.unknown ==> 0
      parser.parseErrors ==> 0
    }
    test("flag absent with default value") {
      val parser = new TestParser
      val n1 = parser.param[String]("--n1", "false", flag = true)
      parser.parseResult(Nil)

      parser.missing ==> 0
      parser.unknown ==> 0
      parser.parseErrors ==> 0
      n1.value ==> "false"
    }
    test("named, not embedded") {
      val parser = new TestParser
      val n1 = parser.requiredParam[String]("--n1")
      val n2 = parser.requiredParam[String]("--n2")
      val p1 = parser.requiredParam[String]("p1")
      parser.parseResult(
        "--n1" :: "a" :: "--n1" :: "b" :: "c" :: "--n2" :: "d" :: Nil
      )

      parser.missing ==> 0
      parser.unknown ==> 0
      parser.parseErrors ==> 0
      n1.value ==> "b"
      n2.value ==> "d"
      p1.value ==> "c"
    }
    test("alias short") {
      val parser = new TestParser
      val n1 = parser.requiredParam[String]("--n1", aliases = List("-a"))
      parser.parseResult("-a" :: "a" :: Nil)

      parser.missing ==> 0
      parser.unknown ==> 0
      parser.parseErrors ==> 0
      n1.value ==> "a"
    }
    test("alias long") {
      val parser = new TestParser
      val n1 = parser.requiredParam[String]("--n1", aliases = List("--n2"))
      parser.parseResult("--n2" :: "a" :: Nil)

      parser.missing ==> 0
      parser.unknown ==> 0
      parser.parseErrors ==> 0
      n1.value ==> "a"
    }
    test("alias and original") {
      val parser = new TestParser
      val n1 = parser.repeatedParam[String]("--n1", aliases = List("-a", "-b"))
      parser.parseResult("--n1" :: "a" :: "-a" :: "b" :: "-b" :: "c" :: Nil)

      parser.missing ==> 0
      parser.unknown ==> 0
      parser.parseErrors ==> 0
      n1.value ==> List("a", "b", "c")
    }
    test("special flags") {
      test("help") {
        val parser = new TestParser
        // the --help flag has top priority; it can appear anywhere on the command line
        parser.parseResult(
          List("a", "b", "c", "--name", "--help", "foo", "--bar")
        ) ==> argparse.ParseResult.EarlyExit
      }
    }
    test("short options") {
      val parser = new TestParser
      test("named args") {
        parser.requiredParam[String]("-p1")
        parser.requiredParam[String]("-p2")
        parser.parseResult("-p1=a" :: "-p2=b" :: Nil)

        parser.missing ==> 0
        parser.unknown ==> 0
      }
      test("named args missing") {
        parser.requiredParam[String]("-p1")
        parser.requiredParam[String]("-p2")
        parser.parseResult(Nil)

        parser.missing ==> 2
        parser.unknown ==> 0
      }
      test("named args missing default") {
        parser.param[String]("-p1", "a")
        parser.requiredParam[String]("-p2")
        parser.parseResult(Nil)

        parser.missing ==> 1
        parser.unknown ==> 0
      }
      test("named args too many") {
        parser.requiredParam[String]("-p1")
        parser.requiredParam[String]("-p2")
        parser.parseResult("-p2=a" :: "-p3=c" :: Nil)

        parser.missing ==> 1
        parser.unknown ==> 1
      }
      test("flags") {
        val a = parser.param[Boolean]("-a", default = false, flag = true)
        val b = parser.param[Boolean]("-b", default = false, flag = true)
        val c = parser.param[Boolean]("-c", default = false, flag = true)


        test("all") {
          parser.parseResult("-abc" :: Nil)

          parser.missing ==> 0
          parser.unknown ==> 0
          a.value ==> true
          b.value ==> true
          c.value ==> true
        }
        test("order") {
          parser.parseResult("-cab" :: Nil)

          parser.missing ==> 0
          parser.unknown ==> 0
          a.value ==> true
          b.value ==> true
          c.value ==> true
        }
        test("partial1") {
          parser.parseResult("-cb" :: Nil)

          parser.missing ==> 0
          parser.unknown ==> 0
          a.value ==> false
          b.value ==> true
          c.value ==> true
        }
        test("partial2") {
          parser.parseResult("-cb" :: "-a" :: Nil)

          parser.missing ==> 0
          parser.unknown ==> 0
          a.value ==> true
          b.value ==> true
          c.value ==> true
        }
      }
      test("options1") {
        val param = parser.param[String]("-a", "unset")
        test("no value") {
          parser.parseResult("-a" :: Nil)
          parser.parseErrors ==> 1
        }
        test("with value") {
          parser.parseResult("-ahello" :: Nil)
          param.value ==> "hello"
        }
        test("with value separate") {
          parser.parseResult("-a" :: "hello" :: Nil)
          param.value ==> "hello"
        }
      }
      test("options2") {
        val param = parser.requiredParam[(String, String)]("-D")
        test("no value") {
          parser.parseResult("-D" :: Nil)
          parser.parseErrors ==> 1
        }
        test("with value") {
          parser.parseResult("-Da=b" :: Nil)
          param.value ==> ("a", "b")
        }
        test("with value separate") {
          parser.parseResult("-D" :: "a=b" :: Nil)
          param.value ==> ("a", "b")
        }
        test("with value embedded") {
          parser.parseResult("-D=a=b" :: Nil)
          param.value ==> ("a", "b")
        }
      }
      test("mixed") {
        val flag = parser.param[Boolean]("-a", default = false, flag = true)
        val param = parser.param[String]("-b", "unset")
        val pos = parser.param[String]("pos", "unset")

        test("ok1") {
          parser.parseResult("-bhello" :: Nil)
          parser.missing ==> 0
          parser.unknown ==> 0
          flag.value ==> false
          param.value ==> "hello"
        }
        test("ok2") {
          parser.parseResult("-bahello" :: Nil)
          parser.missing ==> 0
          parser.unknown ==> 0
          flag.value ==> false
          param.value ==> "ahello"
        }
        test("ok3") {
          parser.parseResult("-b" :: "hello" :: Nil)
          parser.missing ==> 0
          parser.unknown ==> 0
          flag.value ==> false
          param.value ==> "hello"
        }
        test("withflag1") {
          parser.parseResult("-abhello" :: Nil)
          parser.missing ==> 0
          parser.unknown ==> 0
          flag.value ==> true
          param.value ==> "hello"
        }
        test("withflag2") {
          parser.parseResult("-ab" :: "hello" :: Nil)
          parser.parseErrors ==> 1
        }
        test("withflag3") {
          parser.parseResult("-ba" :: "hello" :: Nil)
          flag.value ==> false
          param.value == "a"
          pos.value ==> "hello"
        }
      }
    }
  }
}
