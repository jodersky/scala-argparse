
class Foo

// object api extends argparse.core.Api with argparse.core.MainArgsApi



class yo(base: Int) {

  @argparse.main()
  def foo(value: Int = base * 3, paths: List[os.Path] = Nil) = {
    println(value)
    println(paths)
  }

  // @argparse.main()
  // def bar() = {

  // }

  @argparse.main()
  def baz(q: Int) = {
    println(base * q)
  }

}

val entrypoints = argparse.default.initialize[yo]

def main(args: Array[String]) = {
  val parser = argparse.default.ArgumentParser()

  val base = parser.param(
    "--base",
    1
  )

  entrypoints.addToParser(parser, yo(base.value))

  // entrypoints.entrypoints.foreach{ ep =>
  //   parser.command(ep.name, a => ep.invoke(yo(base.value))(a))
  // }
  parser.parseOrExit(args)


  //println(entrypoints.te)
  //println(entrypoints.methods)



  // parser.param("--fooBar", "")
  // parser.parseOrExit(args)

  // entrypoints.entrypoint.foreach{ ep =>
  //   println(ep.name)
  //   // ep.run(yo(3), args)
  // }


  //entrypoints.entrypoints.head.invoke(yo(2), args)

  //entrypoints.methods.head.run(args)
}
