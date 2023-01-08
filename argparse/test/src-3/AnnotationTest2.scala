import utest._

// At the time of this writing, there are some strange bugs in combining macros
// and givens within utest tests (which are macros themselves). Hence, this
// test suite is distinct from AnnotationTest and defines givens at the top
// level.
object AnnotationTest2 extends TestSuite:

  class Custom
  object api1 extends argparse.core.Api:
    given Reader[Custom] with
      def read(a: String): Reader.Result[Custom] = Reader.Success(Custom())
      def typeName: String = "custom"

  object api2 extends argparse.core.Api

  object app1:
    @api1.command()
    def foo(custom: Custom) = ()

  object app2:
    given api2.Reader[Custom] with
      def read(a: String): api2.Reader.Result[Custom] = api2.Reader.Success(Custom())
      def typeName: String = "custom"

    @api2.command()
    def foo(custom: Custom) = ()

  val tests = Tests{
    test("reader") {
      object wrapper:
        argparse.main(app1, Seq())
    }
    test("in-scope reader") {
      object wrapper2:
        import app2.given
        argparse.main(app2, Seq())
    }
  }
