import utest._

object AnnotationTest extends TestSuite:

  class ParseException extends RuntimeException("parse error")

  object api extends argparse.core.Api:
    override protected def exit(code: Int): Nothing = throw ParseException()

  val tests = Tests {
    test("basic") {
      object app:
        @api.command()
        def foo() = {}
        def main(args: Array[String]) = argparse.main(this, args)
      app.main(Array())
    }
    test("basic2") {
      object app:
        @api.command()
        def foo() = {}
      def main(args: Array[String]) = argparse.main(app, args)

      main(Array())
    }
    test("no args") {
      object app:
        @api.command()
        def foo() = {}
      def main(args: Array[String]) = argparse.main(app, args)

      test("too many arguments") {
        intercept[ParseException] {
          main(Array("a"))
        }
      }
    }
    test("positional") {
      object app:
        @api.command()
        def foo(a: String) = {}
      def main(args: Array[String]) = argparse.main(app, args)

      test("not enough arguments") {
        intercept[ParseException] {
          main(Array())
        }
      }
      test("success") {
        main(Array("a"))
      }
    }
    test("positional2") {
      object app:
        @api.command()
        def foo(a: String, b: Int) = {}
      def main(args: Array[String]) = argparse.main(app, args)

      test("not enough arguments") {
        intercept[ParseException] {
          main(Array())
        }
      }
      test("not enough arguments 2") {
        intercept[ParseException] {
          main(Array("a"))
        }
      }
      test("invalid type") {
        intercept[ParseException] {
          main(Array("a", "b"))
        }
      }
      test("success") {
        main(Array("a", "2"))
      }
    }
    test("named") {
      object app:
        @api.command()
        def foo(a: String = "hello", b: Int = 42) = {}
      def main(args: Array[String]) = argparse.main(app, args)

      test("success1"){
        main(Array())
      }
      test("success2"){
        main(Array("--a", "world", "--b=1"))
      }
      test("too many arguments") {
        intercept[ParseException] {
          main(Array("a"))
        }
      }
    }
    test("flag") {
      object app:
        @api.command()
        def foo(a: Boolean = false) = {}
      def main(args: Array[String]) = argparse.main(app, args)

      test("success1"){
        main(Array())
      }
      test("success2"){
        main(Array("--a=true"))
      }
      test("success3"){
        main(Array("--a"))
      }
      test("success4"){
        main(Array("--a=false"))
      }
      test("too many arguments") {
        intercept[ParseException] {
          main(Array("true"))
        }
      }
      test("too many arguments2") {
        intercept[ParseException] {
          main(Array("false"))
        }
      }
    }
    test("repeated positional") {
      object app:
        @api.command()
        def foo(a: Seq[String]) = {}
      def main(args: Array[String]) = argparse.main(app, args)

      test("success1"){
        main(Array())
      }
      test("success2"){
        main(Array("hello"))
      }
      test("success3"){
        main(Array("hello", "world"))
      }
    }
    test("repeated named") {
      object app:
        @api.command()
        def foo(a: Seq[String] = Seq()) = {}
      def main(args: Array[String]) = argparse.main(app, args)

      test("success1"){
        main(Array())
      }
      test("success2"){
        main(Array("--a", "hello"))
      }
      test("success3"){
        main(Array("--a", "hello", "--a", "world"))
      }
    }
    test("repeated non-seq") {
      object app:
        @api.command()
        def foo(a: List[String]) = {}
      def main(args: Array[String]) = argparse.main(app, args)

      test("success1"){
        main(Array())
      }
      test("success2"){
        main(Array("hello"))
      }
      test("success3"){
        main(Array("hello", "world"))
      }
    }
    test("no mains") {
      object app:
        def foo() = {}
      val err = compileError("def main(args: Array[String]) = argparse.main(app, args)")
      assert(err.msg.contains("No main method found"))
    }
    test("too many mains") {
      object app:
        @api.command()
        def foo() = {}
        @api.command()
        def bar() = {}
      val err = compileError("def main(args: Array[String]) = argparse.main(app, args)")
      assert(err.msg.contains("Too many main methods found"))
    }
    test("no-reader") {
      object app:
        class Foo
        @api.command()
        def foo(data: Foo) = ()
      val err = compileError("def main(args: Array[String]) = argparse.main(app, args)")
      assert(err.msg.contains("No AnnotationTest.api.Reader[app.Foo] available for parameter data"))
    }
  }
