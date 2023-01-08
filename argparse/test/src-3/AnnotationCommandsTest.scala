import utest._

object AnnotationCommandsTest extends TestSuite:

  class ParseException extends RuntimeException("parse error")

  object api extends argparse.core.Api:
    override protected def exit(code: Int): Nothing = throw ParseException()

  val tests = Tests{
    test("commands") {
      object app:

        @api.command()
        class entry():

          @api.command()
          def cmd1() = ()

          @api.command()
          def cmd2(a: Int = 2, b: Int) = ()

      def main(args: Array[String]) = argparse.main(app, args)

      test("cmd1") {
        main(Array("cmd1"))
      }
      test("cmd2") {
        main(Array("cmd2", "--a", "0", "2"))
        main(Array("cmd2", "2"))
      }
      test("cmd2 error") {
        intercept[ParseException]{ // missing arguments
          main(Array("cmd2"))
        }
      }
      test("cmd3") {
        intercept[ParseException]{ // unknown command
          main(Array("cmd3"))
        }
      }
      test("no command") {
        intercept[ParseException]{ // missing command
          main(Array())
        }
      }
    }
    test("nested commands") {
      object app:

        @api.command()
        class entry():

          @api.command()
          class nested():

            @api.command()
            def foo() = ()


      def main(args: Array[String]) = argparse.main(app, args)

      main(Array("nested", "foo"))
    }
    test("commands with outer params") {
      object app:

        @api.command()
        class entry(global: String):

          @api.command()
          def foo(dep: String = global) = ()


      def main(args: Array[String]) = argparse.main(app, args)

      main(Array("b", "foo", "--dep", "c"))
    }
    test("commands with outer default params") {
      appMain(Array("--global", "b", "foo", "--dep", "c"))
    }
  }

  object app:
    @api.command()
    class entry(global: String = "a"): // due to some interaction with utest, this default parameter cannot be defined inside a test

      @api.command()
      def foo(dep: String = global) = ()

  def appMain(args: Array[String]) = argparse.main(app, args)
