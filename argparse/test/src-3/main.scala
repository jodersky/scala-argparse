
class Foo

object api extends argparse.core.Api with argparse.core.MainArgsApi

val entrypoints = api.initialize()

@argparse.main()
def foo(value: Int = 2) = {
  println(value)
}

@argparse.main()
def bar() = {

}

def main(args: Array[String]) = {
  //println(entrypoints.te)
  //println(entrypoints.methods)

  entrypoints.methods.head.run(args)
}
