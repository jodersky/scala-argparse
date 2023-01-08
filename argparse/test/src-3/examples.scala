object api
  extends argparse.core.ParsersApi
  with argparse.core.MacroApi
  with argparse.core.TypesApi
  with argparse.core.ReadersApi



object example1:

  // @api.command()
  // def foo(x: Int) =
  //   wrapper(2)
  //   // println(x)

  @api.command()
  def ops(base: Int = 0) = wrapper(base)
  class wrapper(base: Int):
    @api.command()
    def add(x: Int) = println(base + x)

    @api.command()
    def nested(y: Int = 2) = foo(y)
    class foo(y: Int):
      @api.command()
      def ok() = println("ok")

    @api.command()
    class yo(y: Int):
      @api.command()
      def ok() = println("ok")


  // val mains = argparse.core.Macros.find[example1.type]

  def main(args: Array[String]) = argparse.main(example1, args)
    // val commands = argparse.Command.find(this)


  // def main(args: Array[String]) =
  //   val p = api.ArgumentParser()
  //   val x = p.requiredParam[Int](x)
  //   p.action{
  //     foo(x.value)
  //   }
  //   p.parseOrExit(args)


/////////////////

// object example2:

//   @api.main()
//   @api.command()
//   class wrapper(x: Int):
//     require(x > 1)

//     @api.command()
//     def foo(x: Int) = println(x)

//     @api.command()
//     def bar(y: Int) = println(y)

//     @api.command()
//     class nested(x: Int = 2):
//       @api.command()
//       def foo(x: Int) = println(x)


  //
  //   (parent: () => A)) =>
  //
  //   val foop = api.ArgumentParser
  //   val x = foop.param[Int]("x", parent().x)
  //   foop.action{
  //     parent().foo(x.value)
  //   }
  //   foop
  //
  //  (wrapper: () => A) =>
  //  val np = api.ArgumentParser
  //  val x: np.param....
  //  lazy val instance = nested(x.value)
  //
  //  findEntries[nested].foreach( ep =>
  //    np.addSubparser(ep.name, ep.mkParser(() => instance))
  //  )
  //  np
  //
  //
  //
  //
  //
  //

  // def main(args: Array[String]) =
  //   val p = api.ArgumentParser()
  //   val x = p.requiredParam[Int]("x")
  //   def mknested = nested(x.value)
  //
  //   val foop = p.subparser("foo")
  //   val x = foop.requiredParam[Int]("x", mknested.y)
  //   foop.action{
  //     mknested.foo(x.value)
  //   }
  //
  //   val barp = p.subparser("bar")
  //   val y = barp.requiredParam[Int]("y")
  //   barp.action{
  //     mknested.bar(y.value)
  //   }
  //

  //   val nestedp = p.subparser("nested")
  //   val foop = nestedp.subparser("foo")

