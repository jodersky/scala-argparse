import utest._

object AnnotationTest extends TestSuite {

  class ParseException extends RuntimeException("parse error")

  object parsing extends argparse.core.Api {
    override protected def exit(code: Int): Nothing = throw ParseException()
  }

  val tests = Tests {
    test("basic") {
      object wrapper {
        @parsing.main()
        def foo() = {}
        def main(args: Array[String]) = parsing.dispatch(this, args)
      }
      wrapper.main(Array())
    }
    test("basic2") {
      object wrapper {
        @parsing.main()
        def foo() = {}
      }
      def main(args: Array[String]) = parsing.dispatch(wrapper, args)
      main(Array())
    }
    test("no args") {
      object wrapper {
        @parsing.main()
        def foo() = {}
      }
      def main(args: Array[String]) = parsing.dispatch(wrapper, args)
      test("too many arguments") {
        intercept[ParseException] {
          main(Array("a"))
        }
      }
    }
    test("positional") {
      object wrapper {
        @parsing.main()
        def foo(a: String) = {}
      }
      def main(args: Array[String]) = parsing.dispatch(wrapper, args)
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
      object wrapper {
        @parsing.main()
        def foo(a: String, b: Int) = {}
      }
      def main(args: Array[String]) = parsing.dispatch(wrapper, args)
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
      object wrapper {
        @parsing.main()
        def foo(a: String = "hello", b: Int = 42) = {}
      }
      def main(args: Array[String]) = parsing.dispatch(wrapper, args)
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
      object wrapper {
        @parsing.main()
        def foo(a: Boolean = false) = {}
      }
      def main(args: Array[String]) = parsing.dispatch(wrapper, args)
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
      object wrapper {
        @parsing.main()
        def foo(a: Seq[String]) = {}
      }
      def main(args: Array[String]) = parsing.dispatch(wrapper, args)
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
      object wrapper {
        @parsing.main()
        def foo(a: Seq[String] = Seq()) = {}
      }
      def main(args: Array[String]) = parsing.dispatch(wrapper, args)
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
      object wrapper {
        @parsing.main()
        def foo(a: List[String]) = {}
      }
      def main(args: Array[String]) = parsing.dispatch(wrapper, args)
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
      object wrapper {
        def foo() = {}
      }
      val err = compileError("def main(args: Array[String]) = parsing.dispatch(wrapper, args)")
      assert(err.msg.contains("No main method found"))
    }
    test("too many mains") {
      object wrapper {
        @parsing.main()
        def foo() = {}
        @parsing.main()
        def bar() = {}
      }
      val err = compileError("def main(args: Array[String]) = parsing.dispatch(wrapper, args)")
      assert(err.msg.contains("Too many main methods found"))
    }
    test("parameterized") {
      class wrapper(x: Int) {
        @parsing.main()
        def foo() = x
      }
      def main(args: Array[String]) = parsing.dispatch(wrapper(42), args)
      main(Array())
    }
    test("reader") {
      object wrapper {
        class Foo

        @parsing.main()
        def foo(foo: Foo) = {}
      }
      val err = compileError("def main(args: Array[String]) = parsing.dispatch(wrapper, args)")
      assert(err.msg.startsWith("No AnnotationTest.parsing.Reader[wrapper.Foo] available for parameter foo"))
    }
  }
}
