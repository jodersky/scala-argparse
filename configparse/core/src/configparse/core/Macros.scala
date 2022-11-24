package configparse.core

object Macros:
  import quoted.Expr, quoted.Varargs, quoted.Type, quoted.Quotes

  def treeImpl[Api <: SettingApi, SG: Type]
    (using qctx: Quotes)
    (api: Expr[Api]): Expr[SG => SettingTree] =
    import qctx.reflect.*

    '{
      (instance: SG) =>
        ${
          val tpe = TypeRepr.of[SG]

          val settings: List[Expr[(String, SettingApi#Setting[?])]] = for
            field <- tpe.typeSymbol.memberFields
            fieldTpe = tpe.select(field)
            if fieldTpe <:< TypeRepr.of[SettingApi#Setting[_]]
          yield
            val name = Expr(field.name)
            val setting = '{instance}.asTerm.select(field).asExprOf[SettingApi#Setting[_]]
            '{$name -> $setting}

          val groups: List[Expr[(String, SettingTree)]] = for
            field <- tpe.typeSymbol.memberFields
            fieldTpe = tpe.select(field)
            if field.flags.is(Flags.Module) && !field.flags.is(Flags.Synthetic)
          yield
            val name = Expr(field.name)

            fieldTpe.asType match
              case '[t] =>
                val childTree = treeImpl[Api, t](api)
                val childInstance = '{instance}.asTerm.select(field).asExprOf[t]
                '{
                  $name -> $childTree( ${childInstance} )
                }

          '{
            val tree = SettingTree(
              ${Expr.ofList(settings)}.toMap,
              ${Expr.ofList(groups)}.toMap
            )
            tree.traverseNamed((path, s) => s.setPath(path))
            tree
          }
        }
      }

  def derivedImpl[Api <: SettingApi: Type, SG: Type]
    (using qctx: Quotes)
    (prefix: Expr[Api]) =

    '{
      val p = $prefix
      new p.SettingRoot[SG]:
        def tree(s: SG) = ${treeImpl[Api, SG](prefix)}(s).asInstanceOf[SettingTree]
    }

end Macros
