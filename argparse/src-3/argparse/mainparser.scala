package argparse

import scala.quoted._

object MacroUtil {

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

  def getDefaultParams(using qctx: Quotes)(method: qctx.reflect.Symbol): Map[qctx.reflect.Symbol, Expr[Any]] = {
    import qctx.reflect._

    val companion = method.owner.companionModule

    val pairs = for
      (param, idx) <- method.paramSymss.flatten.zipWithIndex
      if (param.flags.is(Flags.HasDefault))
    yield {
      val defaultName = s"${method.name}$$default$$${idx + 1}"
      val tree = Ref(companion.memberMethod(defaultName).head)
      param -> tree.asExpr
    }
    pairs.toMap
  }

  def paramParser(using qctx: Quotes)(
    parser: Expr[ArgParser],
    param: qctx.reflect.Symbol,
    defaults: Map[qctx.reflect.Symbol, Expr[Any]]
  ): Expr[() => _] = {
    import qctx.reflect._

    val annot: Expr[argparse.arg] = param.getAnnotation(TypeRepr.of[argparse.arg].typeSymbol) match {
      case Some(a) => a.asExprOf[argparse.arg]
      case None => '{argparse.arg()} // use default arg() values
    }

    val paramTpe = param.tree.asInstanceOf[ValDef].tpt.tpe

    def summonReader(paramTpe: TypeRepr): Expr[Any] = {
      val readerTpe: Type[Any] = TypeRepr.of[Reader].appliedTo(paramTpe).asType.asInstanceOf[Type[Any]]

      Expr.summon(using readerTpe) match {
        case Some(r) => r
        case None =>
          report.error(s"no ${Type.show(using readerTpe)} available for parameter ${param.name}", param.pos.get)
          '{???}
      }
    }

    defaults.get(param) match {
      // repeated --named
      case Some(default) if paramTpe <:< TypeRepr.of[Seq[_]] =>
        val AppliedType(_, List(inner)) = paramTpe.dealias
        val tpe = inner.asType.asInstanceOf[Type[Any]]
        '{
          $parser.repeatedParam[tpe.Underlying](
            name = ${Expr("--" + kebabify(param.name))},
            aliases = $annot.aliases,
            help = $annot.doc,
            flag = ${Expr(TypeRepr.of[tpe.Underlying] <:< TypeRepr.of[Boolean])}
          )(using ${summonReader(inner)}.asInstanceOf[Reader[tpe.Underlying]])
        }

      // named
      case Some(default) =>
        val tpe = paramTpe.asType.asInstanceOf[Type[Any]]
        '{
          $parser.singleParam[tpe.Underlying](
            name = ${Expr("--" + kebabify(param.name))},
            default = Some(() => $default),
            env = Option($annot.env),
            aliases = $annot.aliases,
            help = $annot.doc,
            flag = ${Expr(TypeRepr.of[tpe.Underlying] <:< TypeRepr.of[Boolean])},
            absorbRemaining = false,
            completer = None,
            bashCompleter = None
          )(using ${summonReader(paramTpe)}.asInstanceOf[Reader[tpe.Underlying]])
        }

      // repeated positional
      case None if paramTpe <:< TypeRepr.of[Seq[_]] =>
        val AppliedType(_, List(inner)) = paramTpe.dealias
        val tpe = inner.asType.asInstanceOf[Type[Any]]
        '{
          $parser.repeatedParam[tpe.Underlying](
            name = ${Expr(kebabify(param.name))},
            aliases = $annot.aliases,
            help = $annot.doc,
            flag = false
          )(using ${summonReader(inner)}.asInstanceOf[Reader[tpe.Underlying]])
        }

      // positional
      case None =>
        val tpe = paramTpe.asType.asInstanceOf[Type[Any]]
        '{
          $parser.singleParam[tpe.Underlying](
            ${Expr(kebabify(param.name))},
            default = None,
            env = Option($annot.env),
            aliases = $annot.aliases,
            help = $annot.doc,
            flag = false,
            absorbRemaining = false,
            completer = None,
            bashCompleter = None
          )(using ${summonReader(paramTpe)}.asInstanceOf[Reader[tpe.Underlying]])
        }
    }
  }

  // call a function with given arguments
  def callFun(using qctx: Quotes)(
    method: qctx.reflect.Symbol,
    args: Expr[Seq[Any]]
  ): Expr[_] = {
    import qctx.reflect._
    val paramss = method.paramSymss


    if (paramss.isEmpty) {
      report.error("At least one parameter list must be declared.", method.pos.get)
      return '{???}
    }

    var idx = 0
    val accesses: List[List[Term]] = for (i <- paramss.indices.toList) yield {
      for (j <- paramss(i).indices.toList) yield {

        val paramTpt = paramss(i)(j).tree.asInstanceOf[ValDef].tpt
        val paramTpe = paramTpt.tpe
        val pt = paramTpe.asType.asInstanceOf[Type[Any]]

        val expr = paramTpe match {
          case AnnotatedType(tpe, annot) if annot.symbol.owner == defn.RepeatedAnnot =>
            report.error("Varargs are not yet supported in main methods. Use a Seq[_] instead.", paramTpt.pos)
            '{???}
          case _ =>
            '{$args(${Expr(idx)}).asInstanceOf[pt.Underlying]}
        }

        idx += 1
        expr.asTerm
      }
    }

    val fct = Ref(method)
    val base = Apply(fct, accesses.head)
    val application: Apply = accesses.tail.foldLeft(base)((lhs, args) => Apply(lhs, args))
    val expr = application.asExpr
    expr
  }

  def parseOrExitSymbol(using qctx: Quotes)(containerTpe: qctx.reflect.Symbol, args: Expr[Iterable[String]]): Expr[Unit] = {
    import qctx.reflect._

    val mainMethods: List[Symbol] = containerTpe.declaredMethods.filter{ m =>
      try {
        m.hasAnnotation(TypeRepr.of[argparse.main].typeSymbol)
      } catch {
        case _: Exception =>
          // Hack Alert: if this macro is expanded as a method in the container
          // itself, then it will lead to a cyclic dependency. This case here
          // catches the cyclic dependency exception and simply ignores the
          // method.
          //
          // TODO: is there a better way to avoid the potential cyclic dependency
          // in the first place?
          false
      }
    }

    val mainMethod = mainMethods match {
      case Nil =>
        report.error("no methods found annotated with @argparse.main")
        return '{???}
      case head :: Nil => head
      case _ =>
        report.error("more than one @argparse.main method found")
        return '{???}
    }
    val defaults = getDefaultParams(mainMethod)

    '{
      val parser = argparse.ArgParser()
      val accessors: Seq[() => Any] = ${Expr.ofSeq(
        for (param <- mainMethod.paramSymss.flatten) yield paramParser(using qctx)('parser, param, defaults)
      )}
      parser.parseOrExit($args.toSeq)
      val results = accessors.map(_())
      ${callFun(using qctx)(mainMethod, 'results)}
      ()
    }
  }

  def parseOrExitExpr(using qctx: Quotes)(container: Expr[_], args: Expr[Iterable[String]]): Expr[Unit] = {
    import qctx.reflect._
    parseOrExitSymbol(using qctx)(container.asTerm.tpe.typeSymbol, args)
  }

  def parseOrExitThis(using qctx: Quotes)(args: Expr[Iterable[String]]): Expr[Unit] = {
    import qctx.reflect._
    parseOrExitSymbol(Symbol.spliceOwner.owner.owner, args)
  }

}

inline def parseOrExit[A](container: A, args: Iterable[String]): Unit = ${MacroUtil.parseOrExitExpr('container, 'args)}

inline def parseOrExit(args: Iterable[String]): Unit = ${MacroUtil.parseOrExitThis('args)}
