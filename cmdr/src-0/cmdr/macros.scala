package cmdr

class main extends scala.annotation.StaticAnnotation

trait MainC {

  private var entrypoint: Array[String] => Unit = null

  def main(args: Array[String]): Unit = {
    if (entrypoint == null) sys.error("Entrypoint not initialized.")
    entrypoint(args)
  }

  inline def initialize(): Unit = ${initializeImpl('[this.type], '{ep => entrypoint = ep})}

}


import scala.quoted._

def initializeImpl[T](
  tpe: Type[T],
  setter: Expr[(Array[String] => Unit) => Unit]
)(using qctx: QuoteContext): Expr[Unit] = {
  import qctx.tasty._

  val methods = tpe.unseal.symbol.methods

  val mains: List[Symbol] = methods.filter(_.annots.exists(_.tpe <:< typeOf[cmdr.main]))

  val main: Symbol = mains match {
    case Nil =>
      error("No main method found. One method in this class must be annotated with `@cmdr.main`", rootPosition)
      return '{???}
    case head :: Nil => head
    case _ =>
      error("Found more than one main method. Exactly one method in this class must be annotated with `@cmdr.main`", rootPosition)
      return '{???}
  }

  val params: List[Symbol] = main.paramsSyms.flatten

  val parser = '{cmdr.ArgParser()}


  // '{
  //   val parser = cmdr.ArgParser()



  // }




  error("main " + main, rootPosition)

  '{???}

}

def mainImpl(obj: Expr[_])(using qctx: QuoteContext) = {
  import qctx.tasty._

  ???

  val sym = obj.unseal.tpe.classSymbol.get

  val methods: List[Symbol] = sym.classMethods

  error(methods.map(_.signature).toString, sym.pos)


  '{???}
}

