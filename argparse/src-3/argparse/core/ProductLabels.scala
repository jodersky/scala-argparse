package argparse.core

case class ProductLabels[A](
  labels: List[String]
)

object ProductLabels:
  import quoted.Expr, quoted.Varargs, quoted.Type, quoted.Quotes

  inline given [A <: Product]: ProductLabels[A] = of[A]

  inline def of[A <: Product]: ProductLabels[A] = ${ofImpl[A]}

  def ofImpl[A <: Product: Type](using qctx: Quotes): Expr[ProductLabels[A]] =
    import qctx.reflect.*

    val tpe = TypeRepr.of[A]

    if tpe.classSymbol.isDefined then
      val labels = for field <- tpe.classSymbol.get.caseFields yield
        Expr(field.name)
      '{
        ProductLabels[A](${Expr.ofList(labels)})
      }
    else
      '{ProductLabels[A](Nil)}

