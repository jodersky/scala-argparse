package cmdr

object Macros {
  import scala.quoted._

  /** `thisIsKebabCase => this-is-kebab-case` */
  def kebabify(camelCase: String): String = {
    val kebab = new StringBuilder
    var prevIsLower = false
    for (c <- camelCase) {
      if (prevIsLower && c.isUpper) {
        kebab += '-'
      }
      kebab += c.toLower
      prevIsLower = c.isLower
    }
    kebab.result()
  }


  def getPdefs[A: Type](using qctx: Quotes)(
    expr: Expr[A],
    reportParseError: Expr[(String, String) => Unit],
    reportMissingArg: Expr[String => Unit]
  ): List[Expr[Parser.ParamDef]] = {
    import qctx.reflect._

    val pdefs = collection.mutable.ListBuffer.empty[Expr[Parser.ParamDef]]

    def traverse(instance: Term, tsym: Symbol, prefix: String = "--"): Unit = {
      import qctx.reflect._

      for (sym <- tsym.declaredFields) {
        if (sym.flags.is(Flags.Module)) {
          traverse(instance.select(sym), sym.moduleClass, prefix + kebabify(sym.name) + ".")
        } else if (sym.flags.is(Flags.Mutable)) {
          val name = prefix + kebabify(sym.name)

          val tpe = sym.tree.asInstanceOf[ValDef].tpt.tpe
          val readerTpe = TypeRepr.of[Reader].appliedTo(tpe)

          val r = tpe.asType.asInstanceOf[Type[Any]]

          val reader: Expr[Reader[Any]] = Implicits.search(readerTpe) match {
            case iss: ImplicitSearchSuccess =>
              iss.tree.asExpr.asInstanceOf[Expr[Reader[Any]]]
            case isf: ImplicitSearchFailure =>
              report.throwError(s"no implicit ${readerTpe.show} found")
          }

          pdefs += '{
            def read(name: String, strValue: String): Unit = {
              ${reader}.read(strValue) match {
                case Left(message) =>
                  ${reportParseError}(name, message)
                  // /sys.error("parse error: " + message)//reportParseError(name, message)
                case Right(value) => ${
                  Assign(instance.select(sym), '{value.asInstanceOf[r.Underlying]}.asTerm).asExpr
                }
              }
            }
            Parser.ParamDef(
              names = Seq(${Expr(name)}),
              parseAndSet = (name, value) => value match {
                case None =>
                  ${reportMissingArg}(name)
                  //sys.error("argument expected")
                case Some(v) => read(name, v)
              },
              missing = () => (), // do nothing
              isFlag = ${Expr(tpe <:< TypeRepr.of[Boolean])},
              repeatPositional = false,
              absorbRemaining = false
            )
          }

        }
      }
    }

    traverse(expr.asTerm, TypeRepr.of[A].typeSymbol)
    pdefs.result()
  }

  def impl[A: Type](using qctx: Quotes)(expr: Expr[A]): Expr[List[Parser.ParamDef]] = {
    Expr.ofList(getPdefs(
      expr,
      '{(name: String, message: String) => sys.error(s"parse oops $name $message")},
      '{(name: String) => sys.error(s"missing arg $name")}
    ))
  }

}

class Ap {

  /** Add parameter definitions for all vars in a class and nested objects.
    *
    * Variables will be set when a corresponding argument is encountered on the
    * commandline. The rules are as follows:
    *
    * - Every variable will be assigned a named parameter (starting with '--')
    *
    * - The named parameter is optional. If it is not encountered on the command
    *   line, the variable's default value will be used (since this method
    *   requires a class *instance*, variables cannot be abstract).
    *
    * - For every variable of type A, an implicit Reader[A] must be available.
    *
    * - Boolean variables are parsed as flags.
    *
    * Example:
    *
    * ```
    * class Settings {
    *   var opt1: String = "default1"
    *   var opt2: Int = 42
    *   var enableSystemA: Boolean = false
    *
    *   object http {
    *     var host: String = "localhost"
    *     var port: Int = 2020
    *   }
    *
    *   object monitoring {
    *     var host: String = "localhost"
    *     var port: Int = 3030
    *   }
    * }
    *
    * def main(args: Array[String]): Unit = {
    *   val parser = cmdr.ArgParser()
    *
    *   val settings = parser.settings(Settings())
    *
    *   println("before")
    *   println(settings) // all default values
    *
    *   parser.parse(Array(
    *     "--opt1", "value1", "--enable-system-a", "--monitoring.port", "3031"
    *   ))
    *
    *   println("after")
    *   println(settings) // opt1, enableSystemA and monitoring.port have been changed
    * }
    *
    * ```
    *
    */
  inline def settings[A](a: A) = ${Macros.impl[A]('a)}

}
