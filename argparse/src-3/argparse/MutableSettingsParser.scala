package argparse

object MutableSettingsParser {
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

          val reader: Expr[Reader[Any]] = Implicits.search(readerTpe) match {
            case iss: ImplicitSearchSuccess =>
              iss.tree.asExpr.asInstanceOf[Expr[Reader[Any]]]
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
                case None =>
                  ${reportParseError}(name, "argument expected")
                  Parser.Continue
                case Some(v) =>
                  read(name, v)
                  Parser.Continue
              },
              missing = () => (), // do nothing, all variables have a default value
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
              showDefault = Some(() => ${reader}.show(${instance.select(sym).asExpr})),
              completer = _ => Seq.empty,
              bashCompleter = Reader.BashCompleter.Empty
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
      '{(name: String, message: String) => ${instance}.reporter.reportParseError(name, message)}
    )

    '{
      ${Expr.ofList(pdefs.map(pdef => '{ $instance.addParamDef($pdef) }))}
      ${Expr.ofList(pinfos.map(pinfo => '{ $instance.addParamInfo($pinfo) }))}
      $expr
    }
  }

}
