package argparse.core

case class Command[A](
  name: String,
  makeParser: (() => A) => ParsersApi#ArgumentParser
):
  type Container = A

object Command:

  inline def find[Container]: Seq[Command[Container]] = ${
    CommandMacros.findImpl[Container]
  }

object CommandMacros:
  import quoted.Expr
  import quoted.Quotes
  import quoted.Type

  def mainImpl[Container: Type](using qctx: Quotes)(
    instance: Expr[Container],
    args: Expr[Iterable[String]],
    env: Expr[Map[String, String]]
  ) =
    import qctx.reflect.*
    findAllImpl[Container] match
      case Nil =>
        report.error(s"No main method found in ${TypeRepr.of[Container].show}. The container object must contain exactly one method annotated with @command")
        '{???}
      case head :: Nil =>
        '{
          val parser = $head.makeParser(() => $instance)
          parser.parseOrExit($args, $env)
        }
      case list =>
        report.error(s"Too many main methods found in ${TypeRepr.of[Container].show}. The container object must contain exactly one method annotated with @command")
        '{???}

  def findImpl[Container: Type](using qctx: Quotes) =
    Expr.ofList(findAllImpl[Container])

  def findAllImpl[Container: Type](using qctx: Quotes): List[Expr[Command[Container]]] =
    import qctx.reflect.*
    val CommandAnnot = TypeRepr.of[MacroApi#command]

    val containerTpe = TypeRepr.of[Container].typeSymbol

    // need to handle the case where `findImpl` is called from a top-level
    // function, but commands are wrapped in a top-level annotated class
    val isTopLevel = containerTpe.owner.isPackageDef && containerTpe.name.endsWith("$package$")

    val methods = containerTpe.memberMethods
    val classes = if isTopLevel then
      containerTpe.owner.memberTypes.filter(_.isClassDef)
    else
      containerTpe.memberTypes.filter(_.isClassDef)

    for
      sym <- (methods ++ classes)
      annots = sym.annotations
      annot = annots.find(_.tpe <:< CommandAnnot)
      if annot.isDefined
    yield
      val TypeRef(apiType: TermRef, _) = annot.get.tpe
      val api = Ref.term(apiType)

      val method =
        if sym.isClassDef then
          sym.primaryConstructor
        else
          sym

      val doc = DocComment.extract(sym.docstring.getOrElse(""))

      apiType.asType match
        case '[t] if TypeRepr.of[t] <:< TypeRepr.of[MacroApi] =>
          makeCommand[Container, MacroApi](api.asExprOf[MacroApi], method, sym.name, doc, isTopLevel)
        case '[t] =>
          report.error(s"wrong API ${Type.show[t]}")
          '{???}

  def getDefaultParams(using qctx: Quotes)(instance: qctx.reflect.Term, method: qctx.reflect.Symbol): Map[qctx.reflect.Symbol, qctx.reflect.Term] =
    import qctx.reflect.*

    val pairs = for
      (param, idx) <- method.paramSymss.flatten.zipWithIndex
      if (param.flags.is(Flags.HasDefault))
    yield {
      val tree = if method.isClassConstructor then
        val defaultName = s"$$lessinit$$greater$$default$$${idx + 1}"
        Select(
          Select(instance, method.owner.companionModule),
          method.owner.companionModule.memberMethod(defaultName).head
        )
      else
        val defaultName = s"${method.name}$$default$$${idx + 1}"
        Select(instance, method.owner.memberMethod(defaultName).head)
      param -> tree
    }
    pairs.toMap

  // term.apply(List(argss(0)(0),...,argss(0)(N)), ..., List(argss(M)(0),...,argss(M)(N)))
  def call(using qctx: Quotes)(
    term: qctx.reflect.Term,
    paramss: List[List[qctx.reflect.TypeRepr]],
    argss: Expr[Seq[Seq[?]]]
  ): qctx.reflect.Term =
    import qctx.reflect.*

    val accesses =
      for i <- paramss.indices.toList yield
        for j <- paramss(i).indices.toList yield
          paramss(i)(j).asType match
            case '[t] =>
              '{$argss(${Expr(i)})(${Expr(j)}).asInstanceOf[t]}.asTerm

    val application = accesses.foldLeft(term)((lhs, args) => Apply(lhs, args))
    application

  def makeCommand[Container: Type, Api <: MacroApi: Type](using qctx: Quotes)(
    api: Expr[Api],
    method: qctx.reflect.Symbol,
    name: String, // name is separate because method name is not always representative (e.g. if method is class constructor)
    doc: DocComment,
    isTopLevel: Boolean
  ): Expr[Command[Container]] =
    import qctx.reflect.*

    val rtpe = method.tree.asInstanceOf[DefDef].returnTpt.tpe
    val ptpes = method.paramSymss.map(_.map(_.tree.asInstanceOf[ValDef].tpt.tpe))
    val inner = rtpe.asType match
      case '[t] => findAllImpl[t]

    val printerTpe = TypeSelect(api.asTerm, "Printer").tpe.appliedTo(List(rtpe))
    val printer = Implicits.search(printerTpe) match
      case iss: ImplicitSearchSuccess =>
        iss.tree
      case other =>
        report.error(s"No ${printerTpe.show} available for ${method.name}.", method.pos.get)
        '{???}.asTerm

    val makeParser = '{
      (instance: () => Container) =>
        val parser = $api.ArgumentParser(description = ${Expr(doc.paragraphs.mkString("\n"))})

        val args: Seq[Seq[() => ?]] = ${
          val defaults = getDefaultParams(using qctx)('{instance()}.asTerm, method)

          val accessors =
            for paramList <- method.paramSymss yield
              val ls = for param <- paramList yield
                val overrideName: Option[Expr[String]] =
                  param.getAnnotation(TypeRepr.of[argparse.name].typeSymbol).map(
                    a => '{${a.asExprOf[argparse.name]}.name}
                  )

                val aliasAnnot: Expr[Seq[String]] = param.getAnnotation(TypeRepr.of[argparse.alias].typeSymbol) match
                  case Some(a) => '{${a.asExprOf[argparse.alias]}.aliases}
                  case None => '{Seq()}

                val envAnnot: Expr[Option[String]] = param.getAnnotation(TypeRepr.of[argparse.env].typeSymbol) match
                  case Some(a) => '{Some(${a.asExprOf[argparse.env]}.env)}
                  case None => '{None}


                // TODO: replace with `param.termRef.widenTermRefByName` when upgrading scala version
                val paramTpe = param.tree.asInstanceOf[ValDef].tpt.tpe

                def summonReader(tpe: TypeRepr): Term =
                  val readerType =
                    TypeSelect(
                      api.asTerm,
                      "Reader"
                    ).tpe.appliedTo(List(tpe))
                  Implicits.search(readerType) match
                    case iss: ImplicitSearchSuccess => iss.tree
                    case other =>
                      report.error(s"No ${readerType.show} available for parameter ${param.name}.", param.pos.get)
                      '{???}.asTerm

                paramTpe match
                  case t if t =:= TermRef(api.asTerm.tpe, "ArgumentParser") =>
                    '{() => parser}
                  case t if t <:< TypeRepr.of[Iterable[?]] =>
                    val AppliedType(_, List(inner)) = paramTpe.dealias

                    val reader = summonReader(inner)

                    // Note: for some reason we cannot access `inner` in the
                    // expression. It will lead to a compiler crash with a
                    // message "key not found: method $anonfun". To work around
                    // this, we duplicate code and introduce a new top-level
                    // case.
                    val innerBoolean = inner =:= TypeRepr.of[Boolean]

                    defaults.get(param) match
                      case Some(_) if innerBoolean => // --named repeated flag
                        '{
                          val p = $api
                          val arg = parser.asInstanceOf[p.ArgumentParser].repeatedParam[Any](
                            name = ${overrideName match
                              case None => Expr(TextUtils.kebabify(s"--${param.name}"))
                              case Some(name) => name},
                            aliases = ${aliasAnnot},
                            help = ${Expr(doc.params.getOrElse(param.name, ""))},
                            flag = true,
                            endOfNamed = false
                          )(using ${reader.asExpr}.asInstanceOf[p.Reader[Any]])
                          () => arg.value
                        }
                      case Some(_) => // --named repeated
                        '{
                          val p = $api
                          val arg = parser.asInstanceOf[p.ArgumentParser].repeatedParam[Any](
                            name = ${overrideName match
                              case None => Expr(TextUtils.kebabify(s"--${param.name}"))
                              case Some(name) => name},
                            aliases = ${aliasAnnot},
                            help = ${Expr(doc.params.getOrElse(param.name, ""))},
                            flag = false,
                            endOfNamed = false
                          )(using ${reader.asExpr}.asInstanceOf[p.Reader[Any]])
                          () => arg.value
                        }
                      case None => // positional repeated
                        '{
                          val p = $api
                          val arg = parser.asInstanceOf[p.ArgumentParser].repeatedParam[Any](
                            name = ${overrideName match
                              case None => Expr(TextUtils.kebabify(param.name))
                              case Some(name) => name},
                            aliases = ${aliasAnnot},
                            help = ${Expr(doc.params.getOrElse(param.name, ""))},
                            flag = false,
                            endOfNamed = false
                          )(using ${reader.asExpr}.asInstanceOf[p.Reader[Any]])
                          () => arg.value
                        }
                  case t =>
                    val reader = summonReader(t)

                    defaults.get(param) match
                      case Some(default) => // --named
                        '{
                          val p = $api
                          val arg = parser.asInstanceOf[p.ArgumentParser].singleParam[Any](
                            name = ${overrideName match
                              case None => Expr(TextUtils.kebabify(s"--${param.name}"))
                              case Some(name) => name},
                            default = Some(() => ${default.asExpr}),
                            env = ${envAnnot},
                            aliases = ${aliasAnnot},
                            help = ${Expr(doc.params.getOrElse(param.name, ""))},
                            flag = ${Expr(t =:= TypeRepr.of[Boolean])},
                            endOfNamed = false,
                            interactiveCompleter = None,
                            standaloneCompleter = None,
                            argName = None
                          )(using ${reader.asExpr}.asInstanceOf[p.Reader[Any]])
                          () => arg.value
                        }
                      case None => // positional
                        '{
                          val p = $api
                          val arg = parser.asInstanceOf[p.ArgumentParser].singleParam[Any](
                            name = ${overrideName match
                              case None => Expr(TextUtils.kebabify(param.name))
                              case Some(name) => name},
                            default = None,
                            env = ${envAnnot},
                            aliases = ${aliasAnnot},
                            help = ${Expr(doc.params.getOrElse(param.name, ""))},
                            flag = false,
                            endOfNamed = false,
                            interactiveCompleter = None,
                            standaloneCompleter = None,
                            argName = None
                          )(using ${reader.asExpr}.asInstanceOf[p.Reader[Any]])
                          () => arg.value
                        }
              Expr.ofSeq(ls)
            end for
          Expr.ofSeq(accessors)
        }

        def callOrInstantiate() = try
          val results = args.map(_.map(_()))
          ${
            if method.isClassConstructor && isTopLevel then
              call(using qctx)(
                New(method.tree.asInstanceOf[DefDef].returnTpt).select(method),
                ptpes,
                'results
              ).asExpr
            else if method.isClassConstructor && !isTopLevel then
              '{
                val outer = instance()
                ${
                  call(using qctx)(
                    New(TypeSelect('{outer}.asTerm, rtpe.typeSymbol.name)).select(method),
                    ptpes,
                    'results
                  ).asExpr
                }
              }
            else
              call(using qctx)(
                Select('{instance()}.asTerm, method),
                ptpes,
                'results
              ).asExpr
          }
        catch
          case t: Throwable =>
            $api.handleError(t)

        ${
          if inner.isEmpty then
            '{
              parser.action{
                val p = $api
                val pr = ${printer.asExpr}.asInstanceOf[p.Printer[Any]]
                pr.print(
                  callOrInstantiate(),
                  System.out
                )
              }
              parser
            }
          else
            '{
              val commands = ${Expr.ofList(inner)}
              for cmd <- commands do
                parser.addSubparser(
                  cmd.name,
                  cmd.makeParser(() => callOrInstantiate().asInstanceOf[cmd.Container])
                )
              parser
            }
        }
    }
    '{
      Command(
        ${Expr(TextUtils.kebabify(name))},
        $makeParser
      )
    }

  end makeCommand
