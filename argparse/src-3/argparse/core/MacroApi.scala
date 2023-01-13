package argparse.core

trait MacroApi extends TypesApi with ParsersApi:

  // this will be used once macro-annotations are released
  // class main() extends annotation.StaticAnnotation

  class command() extends annotation.StaticAnnotation
  class unknownCommand() extends annotation.StaticAnnotation

  // these annotations are not bound to a specific API bundle, but we add
  // forwarders for convenience
  type name = argparse.name
  val name = argparse.name
  type alias = argparse.alias
  val alias = argparse.alias
  type env = argparse.env
  val env = argparse.env


object Macros:
  import scala.quoted.Expr
  import scala.quoted.Quotes
  import scala.quoted.Type

  // inline def findCommands[Container]: Seq[Command[Container]] = ${findCommandsImpl[Container]}

  inline def find[Container]: Unit = ${findImpl[Container]}

  // call a function with given arguments
  def callFun(using qctx: Quotes)(
    instance: qctx.reflect.Term,
    method: qctx.reflect.Symbol,
    args: Expr[Seq[Any]]
  ): Expr[_] =
    import qctx.reflect._
    val paramss = method.paramSymss

    if paramss.isEmpty then
      report.error("At least one parameter list must be declared.", method.pos.get)
      return '{???}

    var idx = 0
    val accesses: List[List[Term]] = for i <- paramss.indices.toList yield
      for j <- paramss(i).indices.toList yield

        val paramTpt = paramss(i)(j).tree.asInstanceOf[ValDef].tpt
        val paramTpe = paramTpt.tpe
        val pt = paramTpe.asType.asInstanceOf[Type[Any]]

        val expr = paramTpe match
          case AnnotatedType(tpe, annot) if annot.symbol.owner == defn.RepeatedAnnot =>
            report.error("Varargs are not yet supported in main methods. Use a Seq[_] instead.", paramTpt.pos)
            '{???}
          case _ =>
            '{$args(${Expr(idx)}).asInstanceOf[pt.Underlying]}

        idx += 1
        expr.asTerm

    val fct = Select(instance, method)
    val base = Apply(fct, accesses.head)
    val application: Apply = accesses.tail.foldLeft(base)((lhs, args) => Apply(lhs, args))
    val expr = application.asExpr
    expr
  end callFun

  def getDefaultParamValues(using qctx: Quotes)(instance: qctx.reflect.Term, method: qctx.reflect.Symbol): Map[qctx.reflect.Symbol, qctx.reflect.Term] = {
    import qctx.reflect._

    val pairs = for
      (param, idx) <- method.paramSymss.flatten.zipWithIndex
      if (param.flags.is(Flags.HasDefault))
    yield {
      val defaultName = s"${method.name}$$default$$${idx + 1}"
      val tree = Select(instance, method.owner.memberMethod(defaultName).head)
      param -> tree
    }
    pairs.toMap
  }

  //   class A:
  //     @api.command()
  //     def foo(x: Int) = println(x)
  //
  // becomes
  //
  //   (instance: () => A)) =>
  //
  //   val foop = api.ArgumentParser
  //   val x = foop.param[Int]("x", instance().x)
  //   foop.action{
  //     instance().foo(x.value)
  //   }
  //   foop
  def invokeMethod[Container: Type, Api <: ParsersApi: Type](using qctx: Quotes)(
    api: Expr[Api]
  ) =
    '{
      (instance: () => Container) => {
        val parser = $api.ArgumentParser()
        parser.action{
          instance()
        }
        parser
      }
    }

  //
  //   class A:
  //     @api.command()
  //     class commands(x: Int):
  //       ...
  //
  // becomes
  //
  //    (instance: () => A) =>
  //    val parser = api.ArgumentParser
  //    val x = parser.param....
  //    lazy val instance2 = commands(x.value)
  //
  //    findEntries[nested].foreach( ep =>
  //      np.addSubparser(ep.name, ep.makekParser(() => instance2))
  //    )
  //    parser
  // def invokeClass[Container: Type, Api <: ParsersApi: Type](using qctx: Quotes)(

  // def makeCommand[Container: Type](
  //   name: String,
  //   makeParser: (() => A) => ParsersApi#ArgumentParser
  // )


  def findImpl[Container: Type](using qctx: Quotes): Expr[Unit] =
    import qctx.reflect.*

    val commandAnnot = TypeRepr.of[MacroApi#command]

    val methods = TypeRepr.of[Container].typeSymbol.declaredMethods

    for
      method <- methods
      annots = method.annotations
      annot = annots.find(_.tpe <:< commandAnnot)
      if annot.isDefined
    yield
      val TypeRef(api: TermRef, _) = annot.get.tpe
      report.error((api <:< TypeRepr.of[argparse.core.ParsersApi]).toString)

      // val Apply(Select(api, _), _) = annot.get

      report.error(Ref.term(api).toString())

    '{???}

  // def findCommandsImpl[Container: Type](qctx: Quotes): Expr[Seq[Command[Container]]] =
  //   import qctx.reflect.*

  //   val annot = TypeRepr.of[MacroApi#command]

  //   def filterAnnot(syms: Seq[Symbol]) = syms.filter(_.hasAnnotation(annot.typeSymbol))

  //   val methods = TypeRepr.of[Container].typeSymbol.declaredMethods
  //   val classes = TypeRepr.of[Container].typeSymbol.declaredTypes.filter(_.isClassDef)

  //   ???



