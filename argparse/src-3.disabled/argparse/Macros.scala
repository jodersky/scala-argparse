package argparse

import scala.quoted.Quotes
import scala.quoted.Expr
import scala.quoted.Type

class Macros(using qctx: Quotes) {
  import qctx.reflect._

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

  private def methodArgs[R: Type](
    parser: Expr[ArgumentParser],
    prefix: Expr[String],
    params: List[Symbol],
    builder: Term
  ): Expr[() => R] = {
    val defaultValues = for ((sym, idx) <- params.zipWithIndex) yield {
      if (sym.flags.is(Flags.HasDefault)) {
        val method = sym.owner
        val methodName = method.name
          .replace("<", "$less")
          .replace(">", "$greater")
        val defaultName = s"${methodName}$$default$$${idx + 1}"
        val companion = method.owner.companionModule
        val tree = Ref(companion.memberMethod(defaultName).head)
        Some(tree)
      } else {
        None
      }
    }

    val accessors: List[Expr[() => Any]] = for ((sym, default) <- params.zip(defaultValues)) yield {
      val annot: Expr[argparse.arg] = sym.getAnnotation(TypeRepr.of[argparse.arg].typeSymbol) match {
        case Some(a) => a.asExprOf[argparse.arg]
        case None => '{argparse.arg()} // use default arg() values
      }

      val pname = '{$prefix + ${Expr(kebabify(sym.name))}}
      val paliases = '{$annot.aliases} // TODO: handle nested aliases?
      val ptype = sym.tree.asInstanceOf[ValDef].tpt.tpe
      val isFlag = Expr(ptype <:< TypeRepr.of[Boolean])

      ptype.asType match { case '[t] =>
        Expr.summon[Reader[t]] match {
          case Some(reader) =>
            default match {
              case None =>
                '{
                  $parser.requiredParam[t](
                    name = $pname,
                    env = $annot.env,
                    aliases = $paliases,
                    help = $annot.doc,
                    flag = $isFlag
                  )(using $reader)
                }
              case Some(value) =>
                '{
                  $parser.param[t](
                    name = $pname,
                    default = ${value.asExprOf[t]},
                    env = $annot.env,
                    aliases = $paliases,
                    help = $annot.doc,
                    flag = $isFlag
                  )(using $reader)
                }
            }
          case None if ptype.typeSymbol.flags.is(Flags.Case) =>
            caseClassParser[t](parser, '{$pname + "."})
          case None =>
            report.error(s"no ${Type.show[Reader[t]]} available for parameter ${sym.name}", sym.pos.get)
            '{???}
        }
      }
    }
    val e = '{
      val args: Seq[() => Any] = ${Expr.ofSeq(accessors)}
      () => ${
        val args1 = for ((param, i) <- params.zipWithIndex) yield {
          param.tree.asInstanceOf[ValDef].tpt.tpe.asType match { case '[t] =>
            '{args(${Expr(i)})().asInstanceOf[t]}.asTerm
          }
        }
        val application = Apply(builder, args1)
        application.asExprOf[R]
      }
    }
    //System.err.println(e.show)
    e
  }

  def caseClassParser[A: Type](
    parser: Expr[ArgumentParser],
    prefix: Expr[String]
  ): Expr[() => A] = {
    val tsym = TypeRepr.of[A].typeSymbol
    methodArgs[A](
      parser,
      prefix,
      tsym.primaryConstructor.paramSymss.head, // TODO: support more than one param list?
      Select(New(TypeTree.of[A]), tsym.primaryConstructor)
    )
  }

  def lambdaParser(fun0: Expr[Any]): Expr[Seq[String] => Unit] = {
    val Inlined(_, _, fun1) = fun0.asTerm

    def findMethod(a: Tree): (Symbol, List[List[Symbol]]) = a match {
      case Apply(id @ Ident(_), params) =>
        (id.symbol, params.map(_.symbol) :: Nil)
      case Apply(other, params) =>
        val (sym, argss) = findMethod(other)
        (sym, argss ::: List(params.map(_.symbol)))
      case _ =>
        (Symbol.noSymbol, Nil)
    }

    val (l, lparams, lbody) = fun1 match {
      case l @ Lambda(params, body) =>
        (l, params, body)
      case other =>
        report.error("can only be applied to lambda", fun0)
        return '{???}
    }

    val (method, margss) = findMethod(lbody)
    val mparamss = method.paramSymss

    // Find the method parameters which are given as arguments from lambda
    // parameters. These will be used to derive a command line.
    val params = collection.mutable.ListBuffer.empty[Symbol]
    for (lparam <- lparams) {
      for ((mparams, margs) <- mparamss.zip(margss)) {
        for ((mparam, marg) <- mparams.zip(margs)) {
          if (marg == lparam.symbol) {
            params += mparam
          }
        }
      }
    }


    '{
      (args: Seq[String]) => {
        val parser = argparse.ArgumentParser()
        val arg = ${
          methodArgs[Any](
            'parser,
            '{"--"},
            params.toList,
            Select.unique(fun1, "apply")
          )
        }
        parser.parseOrExit(args)
        arg()
      }
    }
  }

}

// Derive a command line for calling a lambda which calls a method
//
// Impl: (TODO: disabled for now)
// def mainImpl(fun: Expr[Any])(using Quotes) = {
//   Macros().lambdaParser(fun)
// }
//
// inline def mainMacro(inline fun: Any): Seq[String] => Unit = ${mainImpl('fun)}
//
// E.g.
//
// def foo(extra: String)(x: Int, y: Int) = {
//   println(extra)
//   println(x * 2)
// }
//
// def main(args: Array[String]) = {
//   val parser = argparse.ArgumentParser()
//   val extra = parser.param[String]("--extra", default = "hello")
//   parser.command("foo", argparse.mainMacro(foo(extra())(_, _)))
//   parser.parseOrExit(args)
// }
