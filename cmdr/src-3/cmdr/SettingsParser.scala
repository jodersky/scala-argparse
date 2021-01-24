package cmdr

object SettingsParser {
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
    reportParseError: Expr[(String, String) => Unit], // (param name, parse error message)
    reportMissingArg: Expr[String => Unit] // (param name)
  ): (List[Expr[Parser.ParamDef]], List[Expr[ArgParser.ParamInfo]]) = {
    import qctx.reflect._

    val pdefs = collection.mutable.ListBuffer.empty[Expr[Parser.ParamDef]]
    val pinfos = collection.mutable.ListBuffer.empty[Expr[ArgParser.ParamInfo]]

    def traverse(instance: Term, tsym: Symbol, prefix: String): Unit = {
      import qctx.reflect._

      for (sym <- tsym.memberFields) {
        if (sym.flags.is(Flags.Module)) {
          traverse(instance.select(sym), sym.moduleClass, prefix + kebabify(sym.name) + ".")
        } else if (sym.flags.is(Flags.Mutable)) {
          val name: String = prefix + kebabify(sym.name)

          val tpe = sym.tree.asInstanceOf[ValDef].tpt.tpe
          val readerTpe = TypeRepr.of[Reader].appliedTo(tpe)

          val r = tpe.asType.asInstanceOf[Type[Any]]

          val reader: Expr[Reader[_]] = Implicits.search(readerTpe) match {
            case iss: ImplicitSearchSuccess =>
              iss.tree.asExpr.asInstanceOf[Expr[Reader[_]]]
            case isf: ImplicitSearchFailure =>
              report.throwError(s"no implicit ${readerTpe.show} found")
          }

          def isFlag: Boolean = tpe <:< TypeRepr.of[Boolean]

          pdefs += '{
            def read(name: String, strValue: String): Unit = {
              ${reader}.read(strValue) match {
                case e: Reader.Error =>
                  ${reportParseError}(name, e.message)
                case s: Reader.Success[_] => ${
                  Assign(instance.select(sym), '{s.value.asInstanceOf[r.Underlying]}.asTerm).asExpr
                }
              }
            }
            Parser.ParamDef(
              names = Seq(${Expr(name)}),
              parseAndSet = (name, value) => value match {
                case None => ${reportMissingArg}(name)
                case Some(v) => read(name, v)
              },
              missing = () => (), // do nothing
              isFlag = ${Expr(isFlag)},
              repeatPositional = false,
              absorbRemaining = false
            )
          }

          val comment: String = sym.docstring.getOrElse("")
          val help = "help:(.*)".r.findFirstMatchIn(comment).map(m => m.group(1).trim).getOrElse("")
          val env = "env:(.*)".r.findFirstMatchIn(comment).map(m => m.group(1).trim)

          pinfos += '{
            ArgParser.ParamInfo(
              isNamed = true, // settings always have params that start with --
              names = Seq(${Expr(name)}),
              isFlag = ${Expr(isFlag)},
              repeats = false,
              env = ${Expr(env)},
              description = ${Expr(help)},
              completer = ""
            )
          }
        }
      }
    }

    traverse(expr.asTerm, TypeRepr.of[A].typeSymbol, "--")
    (pdefs.result(), pinfos.result())
  }

  def impl[A: Type](using qctx: Quotes)(instance: Expr[ArgParser], expr: Expr[A]): Expr[A] = {
    val (pdefs, pinfos) = getPdefs(
      expr,
      '{(name: String, message: String) => ${instance}.reportParseError(name, message)},
      '{(name: String) => ${instance}.reportMissing(name)}
    )

    '{
      ${Expr.ofList(pdefs.map(pdef => '{ $instance.addParamDef($pdef) }))}
      ${Expr.ofList(pinfos.map(pinfo => '{ $instance.addParamInfo($pinfo) }))}
      $expr
    }
  }

}

trait SettingsParser { self: ArgParser =>

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
    * ```
    */
  inline def settings[A](a: A): A = ${SettingsParser.impl[A]('this, 'a)}

}
