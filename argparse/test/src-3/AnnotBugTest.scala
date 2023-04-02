import utest._

// This tests a weird compiler crash that would happen if at least two repeated
// parameters were declared and one non-repeated.
object AnnotBugTest extends TestSuite:

  class ParseException extends RuntimeException("parse error")

  object api extends argparse.core.Api:
    override protected def exit(code: Int): Nothing = throw ParseException()

  @api.command()
  class app:

    @api.command()
    def cmd1(z: Int, foo: Seq[String]) = ()

    @api.command()
    def cmd2(y: Seq[Int]) = ()

  def appMain(args: Array[String]) = argparse.main(this, args)

  val tests = Tests{
    test("commands") {

    }
  }
