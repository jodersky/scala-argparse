package argparse.core

import quoted.Expr
import quoted.Quotes
import quoted.Type

case class Command[A](
  name: String,
  makeParser: (() => A) => ParsersApi#ArgumentParser
):
  type Container = A

object Command:

  def entrypointImpl[Container: Type](using Quotes)(
    instance: Expr[Container],
    args: Expr[Iterable[String]],
    env: Expr[Map[String, String]]
  ) =
    val util = MacroUtil()
    import util.qctx.reflect.*

    util.makeCommands[Container] match
      case Nil =>
        report.error(s"No main method found in ${TypeRepr.of[Container].show}. The container object must contain exactly one method annotated with @command")
        '{???}
      case head :: Nil =>
        '{
          val parser = $head.asInstanceOf[Command[Any]].makeParser(() => $instance)
          parser.parseOrExit($args, $env)
        }
      case list =>
        report.error(s"Too many main methods found in ${TypeRepr.of[Container].show}. The container object must contain exactly one method annotated with @command")
        '{???}

class MacroUtil(using val qctx: Quotes):
  import qctx.reflect.*

  def getDefaultParams(instance: Term, method: Symbol): Map[Symbol, Term] =
    val pairs = for
      (param, idx) <- method.paramSymss.flatten.zipWithIndex
      if (param.flags.is(Flags.HasDefault))
    yield
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
    pairs.toMap

  def callOrInstantiate(instance: Term, methodOrClass: Symbol, argss: Expr[List[List[?]]]): Term =
    val base: Term = if methodOrClass.isClassDef then
      //Select.unique(New(instance.select(methodOrClass).tpe)), "<init>")
      Select.unique(New(Inferred(instance.tpe.select(methodOrClass))), "<init>")
    else
      instance.select(methodOrClass)

    val paramss = if methodOrClass.isClassDef then
      methodOrClass.primaryConstructor.paramSymss
    else
      methodOrClass.paramSymss

    val accesses =
      for i <- paramss.indices.toList yield
        for j <- paramss(i).indices.toList yield
          paramss(i)(j).termRef.widenTermRefByName.asType match
            case '[t] =>
              '{$argss(${Expr(i)})(${Expr(j)}).asInstanceOf[t]}.asTerm

    val application = accesses.foldLeft(base)((lhs, args) => Apply(lhs, args))
    application

  // Find all methods and class defs which have been annotated with a command
  // annotation from any api trait
  //
  // return a reference to the instance of the API trait and the annotated symbol
  def findAnnotated(tpe: TypeRepr): List[(Ref, Symbol)] =
    val CommandAnnot = TypeRepr.of[MacroApi#command]

    // is the annotated type the synthetically-generated class for top-level methods?
    val isTopLevel = //!tpe.typeSymbol.maybeOwner.exists &&
      tpe.typeSymbol.name.endsWith("$package$")

    val methods = tpe.typeSymbol.methodMembers
    val classes = if isTopLevel then
      tpe.typeSymbol.owner.typeMembers
    else
      tpe.typeSymbol.typeMembers

    for
      sym <- (methods ++ classes)
      if sym.isDefDef || (sym.isClassDef && !sym.flags.is(Flags.Module))
      annots = sym.annotations
      annot = annots.find(_.tpe <:< CommandAnnot)
      if annot.isDefined
    yield
      val TypeRef(apiType: TermRef, _) = annot.get.tpe: @unchecked
      val api = Ref.term(apiType)
      (api, sym)


  def makeCommands[C: Type]: List[Expr[Command[_]]] =
    val containerTpe = TypeRepr.of[C]
    for (api, sym) <- findAnnotated(containerTpe) yield
      makeCommandForSym(api, sym)

  def makeCommandForSym[C: Type](api: Ref, sym: Symbol): Expr[Command[C]] =
    val containerTpe = TypeRepr.of[C]
    val symTpe = containerTpe.select(sym)

    val rtpe = symTpe.widen match
      case MethodType(_, _, r) => r
      case other => other

    val children = rtpe.asType match
      case '[t] => makeCommands[t]

    val apiExpr = api.asExprOf[MacroApi]

    val doc = DocComment.extract(sym.docstring.getOrElse(""))

    val makeParser = if children.isEmpty then
      val printerTpe: TypeRepr = TypeSelect(api, "Printer").tpe.appliedTo(List(rtpe))

      val printer: Expr[_] = Implicits.search(printerTpe) match
        case iss: ImplicitSearchSuccess =>
          iss.tree.asExpr
        case other =>
          report.error(s"No ${printerTpe.show} available for ${sym.name}.", sym.pos.get)
          '{???}
      '{
        (getInstance: () => C) =>
          val parser = $apiExpr.ArgumentParser(description = ${Expr(doc.paragraphs.mkString("\n"))})

          val args: List[List[() => ?]] = ${makeArgs(api, '{getInstance()}.asTerm, sym, 'parser)}

          def call() =
            val instance: C = getInstance()
            val results: List[List[Any]] = args.map(_.map(_()))
            ${
              callOrInstantiate('{instance}.asTerm, sym, 'results).asExpr
            }

          parser.action{
            val p = $apiExpr
            try
              val pr = ${printer}.asInstanceOf[p.Printer[Any]]
              pr.print(
                call(),
                System.out
              )
            catch
              case t: Throwable =>
                p.handleError(t)
          }
          parser
      }
    else
      '{
        (getInstance: () => C) =>
          val parser = $apiExpr.ArgumentParser(description = ${Expr(doc.paragraphs.mkString("\n"))})

          val args: List[List[() => ?]] = ${makeArgs(api, '{getInstance()}.asTerm, sym, 'parser)}

          def instantiateSym() =
            val instance: C = getInstance()
            val results: List[List[Any]] = args.map(_.map(_()))
            ${
              callOrInstantiate('{instance}.asTerm, sym, 'results).asExpr
            }

          val subcommands = ${Expr.ofList(children)}
          for cmd <- subcommands do
            parser.addSubparser(
              cmd.name,
              cmd.makeParser(() => instantiateSym().asInstanceOf[cmd.Container])
            )
          parser
      }

    '{
      Command[C](
        ${Expr(TextUtils.kebabify(sym.name))},
        ${makeParser}
      )
    }

  def makeArgs(api: Ref, instance: Term, sym: Symbol, parser: Expr[_]): Expr[List[List[() => ?]]] =
    val method = if sym.isClassDef then
      sym.primaryConstructor
    else
      sym

    val defaults = getDefaultParams(instance, method)
    val docs = DocComment.extract(sym.docstring.getOrElse(""))

    val ys = for plist <- method.paramSymss yield
      val xs = for param <- plist yield
        makeArg(api, param, defaults, docs, parser)
      Expr.ofList(xs)
    Expr.ofList(ys)

  def makeArg(api: Ref, param: Symbol, defaults: Map[Symbol, Term], docs: DocComment, parser: Expr[_]): Expr[() => ?] =
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

    val paramTpe = param.termRef.widenTermRefByName

    def summonReader(tpe: TypeRepr): Term =
      val readerType =
        TypeSelect(
          api,
          "Reader"
        ).tpe.appliedTo(List(tpe))
      Implicits.search(readerType) match
        case iss: ImplicitSearchSuccess => iss.tree
        case other =>
          report.error(s"No ${readerType.show} available for parameter ${param.name}.", param.pos.get)
          '{???}.asTerm

    val doc = Expr(docs.params.getOrElse(param.name, ""))
    val apiExpr = api.asExprOf[ParsersApi & TypesApi]
    paramTpe match
      case t if t <:< TypeRepr.of[Iterable[?]] =>
        val AppliedType(_, List(inner)) = paramTpe.dealias: @unchecked
        val reader = summonReader(inner)
        val innerBoolean = inner =:= TypeRepr.of[Boolean]
        defaults.get(param) match
          case Some(_) => // --named repeated
            '{
              val p = $apiExpr
              val arg = $parser.asInstanceOf[p.ArgumentParser].repeatedParam[Any](
                name = ${overrideName match
                  case None => Expr(TextUtils.kebabify(s"--${param.name}"))
                  case Some(name) => name},
                aliases = ${aliasAnnot},
                help = ${doc},
                flag = ${Expr(innerBoolean)},
                endOfNamed = false
              )(using ${reader.asExpr}.asInstanceOf[p.Reader[Any]])
              () => arg.value
            }
          case None => // positional repeated
            '{
              val p = $apiExpr
              val arg = $parser.asInstanceOf[p.ArgumentParser].repeatedParam[Any](
                name = ${overrideName match
                  case None => Expr(TextUtils.kebabify(param.name))
                  case Some(name) => name},
                aliases = ${aliasAnnot},
                help = ${doc},
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
              val p = $apiExpr
              val arg = $parser.asInstanceOf[p.ArgumentParser].singleParam[Any](
                name = ${overrideName match
                  case None => Expr(TextUtils.kebabify(s"--${param.name}"))
                  case Some(name) => name},
                default = Some(() => ${default.asExpr}),
                env = ${envAnnot},
                aliases = ${aliasAnnot},
                help = ${doc},
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
              val p = $apiExpr
              val arg = $parser.asInstanceOf[p.ArgumentParser].singleParam[Any](
                name = ${overrideName match
                  case None => Expr(TextUtils.kebabify(param.name))
                  case Some(name) => name},
                default = None,
                env = ${envAnnot},
                aliases = ${aliasAnnot},
                help = ${doc},
                flag = false,
                endOfNamed = false,
                interactiveCompleter = None,
                standaloneCompleter = None,
                argName = None
              )(using ${reader.asExpr}.asInstanceOf[p.Reader[Any]])
              () => arg.value
            }

  end makeArg
