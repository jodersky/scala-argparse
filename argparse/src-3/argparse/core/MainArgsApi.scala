package argparse.core

trait MainArgsApi { types: TypesApi with ParsersApi =>

  // case class Entrypoint(
  //   name: String,
  //   route: main,
  //   build: ArgumentParser => Unit,
  //   invoke0: () => Unit
  // )

  inline def initialize(): EntrypointsMetadata = ${
    EntrypointsMetadata.initializeThis[this.type]
  }

}

class EntrypointsMetadata(
  val methods: Seq[Entrypoint]
)
case class Entrypoint(
  name: String,
  run: Iterable[String] => Unit
)
object EntrypointsMetadata {
  import scala.quoted.Quotes
  import scala.quoted.Expr
  import scala.quoted.Type

  def initializeThis[Api <: TypesApi with ParsersApi: Type](using qctx: Quotes) = {
    import qctx.reflect._
    initializeContainer[Api](Symbol.spliceOwner.owner.owner)
  }

  def initializeContainer[Api <: TypesApi with ParsersApi: Type](using qctx: Quotes)(container: qctx.reflect.Symbol) = {
    import qctx.reflect._

    val mainMethods: List[Symbol] = container.declaredMethods.filter{ m =>
      // try {
        m.hasAnnotation(TypeRepr.of[main].typeSymbol)
      // } catch {
      //   case _: Exception =>
      //     // Hack Alert: if this macro is expanded as a method in the container
      //     // itself, then it will lead to a cyclic dependency. This case here
      //     // catches the cyclic dependency exception and simply ignores the
      //     // method.
      //     //
      //     // TODO: is there a better way to avoid the potential cyclic dependency
      //     // in the first place?
      //     false
      // }
    }

    /*
    val tpe = Applied(
      TypeSelect(
        Ref(TypeRepr.of[Api].typeSymbol.companionModule),
        "Reader"
      ),
      List(TypeTree.of[Int])
    ).tpe.asType.asInstanceOf[Type[Any]]
    */

    val prefix: Expr[Api] = Ref(TypeRepr.of[Api].typeSymbol.companionModule).asExprOf[Api]

    val entrypoints = for method <- mainMethods yield {
      val defaultParamValues = getDefaultParamValues(method)
      val invoke = '{
        val p = $prefix
        val parser = $prefix.ArgumentParser()
        val accessors: Seq[() => Any] = ${
          Expr.ofSeq(
            for param <- method.paramSymss.flatten yield
              paramAccessor(using qctx)(param, defaultParamValues, 'p, 'parser)
          )
        }

        (args: Iterable[String]) => {
          parser.parseOrExit(args)
          val results = accessors.map(_())
          ${callFun(using qctx)(method, 'results)}
          ()
        }
      }
      '{
        Entrypoint(
          ${Expr(method.fullName)},
          $invoke
        )
      }
    }

    // def summonReader(paramTpe: TypeRepr): Expr[Any] = {
    //   val readerTpe: Type[Any] = TypeRepr.of[Api#Reader].appliedTo(paramTpe).asType.asInstanceOf[Type[Any]]

    //   Expr.summon(using readerTpe) match {
    //     case Some(r) => r
    //     case None =>
    //       report.error(s"no ${Type.show(using readerTpe)} available for parameter ${param.name}", param.pos.get)
    //       '{???}
    //   }
    // }


    //te.asTerm.tpe.typeSymbol.companionModule
    //te.asTerm.symbol.companionModule


    //val x = Expr.summon(using tpe)


    //val x = //TypeRepr.of[Api].typeSymbol.companionModule
    // val x = TypeRepr.of[Api].typeSymbol.companionModule.memberFields
    //   .sortBy(_.name)
    //   .mkString("\n")

    val e = '{
      EntrypointsMetadata(
        ${Expr.ofList(entrypoints)}
        //${Expr.ofList(mainMethods.map(s => Expr(s.name)))}
      )
    }
    //System.err.println(e.show)
    e
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

  def getDefaultParamValues(using qctx: Quotes)(method: qctx.reflect.Symbol): Map[qctx.reflect.Symbol, Expr[_]] = {
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

  def paramAccessor[Api <: TypesApi with ParsersApi: Type](using qctx: Quotes)(
    param: qctx.reflect.Symbol,
    defaults: Map[qctx.reflect.Symbol, Expr[Any]],
    prefix: Expr[Api],
    parser: Expr[_] // == prefix.ArgumentParser
  ): Expr[() => _] = {
    import qctx.reflect._

    val annot: Expr[argparse.core.arg] = param.getAnnotation(TypeRepr.of[argparse.core.arg].typeSymbol) match {
      case Some(a) => a.asExprOf[argparse.core.arg]
      case None => '{argparse.core.arg()} // use default arg() values
    }

    val paramTpt = param.tree.asInstanceOf[ValDef].tpt
    val paramTpe = param.tree.asInstanceOf[ValDef].tpt.tpe
    def summonReader(tpe: TypeRepr): Expr[_] = {
      val readerTpe: Type[Any] =
        Applied(
          TypeSelect(
            prefix.asTerm,
            "Reader"
          ),
          List(paramTpt)
        ).tpe.asType.asInstanceOf[Type[Any]]
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
          val p = $prefix
          $parser.asInstanceOf[p.ArgumentParser].repeatedParam[tpe.Underlying](
            name = ${Expr("--" + kebabify(param.name))},
            aliases = $annot.aliases,
            help = $annot.doc,
            flag = ${Expr(TypeRepr.of[tpe.Underlying] <:< TypeRepr.of[Boolean])}
          )(using ${summonReader(inner)}.asInstanceOf[p.Reader[tpe.Underlying]])
        }

      // --named
      case Some(default) =>
        val tpe = paramTpe.asType.asInstanceOf[Type[Any]]
        '{
          val p = $prefix
          $parser.asInstanceOf[p.ArgumentParser].singleParam[tpe.Underlying](
            name = ${Expr("--" + kebabify(param.name))},
            default = Some(() => $default.asInstanceOf[tpe.Underlying]),
            env = Option($annot.env),
            aliases = $annot.aliases,
            help = $annot.doc,
            flag = ${Expr(TypeRepr.of(using tpe) =:= TypeRepr.of[Boolean])},
            absorbRemaining = false,
            completer = None,
            bashCompleter = None
          )(using ${summonReader(paramTpe)}.asInstanceOf[p.Reader[tpe.Underlying]])
        }

      // repeated positional
      case None if paramTpe <:< TypeRepr.of[Seq[_]] =>
        val AppliedType(_, List(inner)) = paramTpe.dealias
        val tpe = inner.asType.asInstanceOf[Type[Any]]
        '{
          val p = $prefix
          $parser.asInstanceOf[p.ArgumentParser].repeatedParam[tpe.Underlying](
            name = ${Expr(kebabify(param.name))},
            aliases = $annot.aliases,
            help = $annot.doc,
            flag = false
          )(using ${summonReader(inner)}.asInstanceOf[p.Reader[tpe.Underlying]])
        }

      // positional
      case None =>
        val tpe = paramTpe.asType.asInstanceOf[Type[Any]]
        '{
          val p = $prefix
          $parser.asInstanceOf[p.ArgumentParser].singleParam[tpe.Underlying](
            ${Expr(kebabify(param.name))},
            default = None,
            env = Option($annot.env),
            aliases = $annot.aliases,
            help = $annot.doc,
            flag = false,
            absorbRemaining = false,
            completer = None,
            bashCompleter = None
          )(using ${summonReader(paramTpe)}.asInstanceOf[p.Reader[tpe.Underlying]])
        }
    }
  }

  // def paramParser(using qctx: Quotes)(
  //   parser: Expr[ArgumentParser],
  //   param: qctx.reflect.Symbol,
  //   defaults: Map[qctx.reflect.Symbol, Expr[Any]]
  // ): Expr[() => _] = {
  //   import qctx.reflect._

  //   val annot: Expr[argparse.core.arg] = param.getAnnotation(TypeRepr.of[argparse.core.arg].typeSymbol) match {
  //     case Some(a) => a.asExprOf[argparse.core.arg]
  //     case None => '{argparse.core.arg()} // use default arg() values
  //   }

  //   val paramTpe = param.tree.asInstanceOf[ValDef].tpt.tpe

  //   def summonReader(paramTpe: TypeRepr): Expr[Any] = {
  //     val readerTpe: Type[Any] = TypeRepr.of[Reader].appliedTo(paramTpe).asType.asInstanceOf[Type[Any]]

  //     Expr.summon(using readerTpe) match {
  //       case Some(r) => r
  //       case None =>
  //         report.error(s"no ${Type.show(using readerTpe)} available for parameter ${param.name}", param.pos.get)
  //         '{???}
  //     }
  //   }

  //   defaults.get(param) match {
  //     // repeated --named
  //     case Some(default) if paramTpe <:< TypeRepr.of[Seq[_]] =>
  //       val AppliedType(_, List(inner)) = paramTpe.dealias
  //       val tpe = inner.asType.asInstanceOf[Type[Any]]
  //       '{
  //         $parser.repeatedParam[tpe.Underlying](
  //           name = ${Expr("--" + kebabify(param.name))},
  //           aliases = $annot.aliases,
  //           help = $annot.doc,
  //           flag = ${Expr(TypeRepr.of[tpe.Underlying] <:< TypeRepr.of[Boolean])}
  //         )(using ${summonReader(inner)}.asInstanceOf[Reader[tpe.Underlying]])
  //       }

  //     // named
  //     case Some(default) =>
  //       val tpe = paramTpe.asType.asInstanceOf[Type[Any]]
  //       '{
  //         $parser.singleParam[tpe.Underlying](
  //           name = ${Expr("--" + kebabify(param.name))},
  //           default = Some(() => $default),
  //           env = Option($annot.env),
  //           aliases = $annot.aliases,
  //           help = $annot.doc,
  //           flag = ${Expr(TypeRepr.of[tpe.Underlying] <:< TypeRepr.of[Boolean])},
  //           absorbRemaining = false,
  //           completer = None,
  //           bashCompleter = None
  //         )(using ${summonReader(paramTpe)}.asInstanceOf[Reader[tpe.Underlying]])
  //       }

  //     // repeated positional
  //     case None if paramTpe <:< TypeRepr.of[Seq[_]] =>
  //       val AppliedType(_, List(inner)) = paramTpe.dealias
  //       val tpe = inner.asType.asInstanceOf[Type[Any]]
  //       '{
  //         $parser.repeatedParam[tpe.Underlying](
  //           name = ${Expr(kebabify(param.name))},
  //           aliases = $annot.aliases,
  //           help = $annot.doc,
  //           flag = false
  //         )(using ${summonReader(inner)}.asInstanceOf[Reader[tpe.Underlying]])
  //       }

  //     // positional
  //     case None =>
  //       val tpe = paramTpe.asType.asInstanceOf[Type[Any]]
  //       '{
  //         $parser.singleParam[tpe.Underlying](
  //           ${Expr(kebabify(param.name))},
  //           default = None,
  //           env = Option($annot.env),
  //           aliases = $annot.aliases,
  //           help = $annot.doc,
  //           flag = false,
  //           absorbRemaining = false,
  //           completer = None,
  //           bashCompleter = None
  //         )(using ${summonReader(paramTpe)}.asInstanceOf[Reader[tpe.Underlying]])
  //       }
  //   }
  // }


}




// @main("foo")
// @main("bar")
// def foo(cfg)(bar)


// @main("foo bar")
// def foo()


// val entry = argparse.initialize()
