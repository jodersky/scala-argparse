
class Foo

object api extends argparse.core.Api {

  given Reader[Foo] with {
    def read(a: String): Reader.Result[Foo] = Reader.Success(Foo())
    def typeName: String = ""
  }

}



class wrapper(base: Int) {

  @argparse.main()
  def command1(value: Int = base * 3, unsupported: Foo) = {
    println(value)
  }

  @argparse.main()
  def command2() = {

  }

  @argparse.main()
  def command3(q: Int) = {
    println(base * q)
  }

}

@argparse.main()
def foo(base: Int = 1) = {
  println(base)
  //wrapper(base)
}

// this should become obsolete once macro annotations are available
// def main(args: Array[String]) = api.main(this, args)

def main(args: Array[String]) = {
  val parser = argparse.default.ArgumentParser()

  val base = parser.param[Int](
    "--base",
    default = 1
  )

  parser.parseOrExit(args)
  foo(base.value)

}

// val entrypoints = argparse.default.initialize[yo]

// def main(args: Array[String]) = {
//   val parser = argparse.default.ArgumentParser()

//   val base = parser.param(
//     "--base",
//     1
//   )
//   parser.commands(yo(base.value))

//   // entrypoints.addToParser(parser, yo(base.value))

//   // entrypoints.entrypoints.foreach{ ep =>
//   //   parser.command(ep.name, a => ep.invoke(yo(base.value))(a))
//   // }
//   parser.parseOrExit(args)


//   //println(entrypoints.te)
//   //println(entrypoints.methods)



//   // parser.param("--fooBar", "")
//   // parser.parseOrExit(args)

//   // entrypoints.entrypoint.foreach{ ep =>
//   //   println(ep.name)
//   //   // ep.run(yo(3), args)
//   // }


//   //entrypoints.entrypoints.head.invoke(yo(2), args)

//   //entrypoints.methods.head.run(args)
// }
