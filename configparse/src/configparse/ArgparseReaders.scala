package configparse

import configparse.core.Ctx
import configparse.core.GroupVisitor
import configparse.core.NoneGroupVisitor
import configparse.core.ArrayVisitor
import configparse.core.NoneArrayVisitor
import configparse.core.Visitor

trait ArgparseReaders extends configparse.core.SettingApi with LowPrio:
  self: argparse.core.TypesApi =>

  given [Elem, Col[Elem] <: Iterable[Elem]](
    using elemReader: SettingReader[Elem],
    factory: collection.Factory[Elem, Col[Elem]]): SettingReader[Col[Elem]] with

    def visitGroup(ctx: Ctx): GroupVisitor[Option[Col[Elem]]] =
      ctx.reporter.error("expected an array, found a setting group")
      NoneGroupVisitor

    def visitString(ctx: Ctx, str: String): Option[Col[Elem]] =
      ctx.reporter.error("expected an array, found a string")
      None

    def visitArray(ctx: Ctx): ArrayVisitor[Option[Col[Elem]]] =
      val builder = factory.newBuilder
      new ArrayVisitor[Option[Col[Elem]]]:
        def visitIndex(ctx: Ctx, idx: Int): Unit = ()
        def subVisitor(): Visitor[?] = elemReader
        def visitValue(ctx: Ctx, value: Any): Unit =
          value.asInstanceOf[Option[Elem]] match
            case None =>
            case Some(elem) => builder.addOne(elem)
        def visitEnd(): Option[Col[Elem]] = Some(builder.result())

trait LowPrio:
  self: ArgparseReaders with argparse.core.TypesApi =>

  given scalarReader[A](using areader: Reader[A]): SettingReader[A] with
    def visitGroup(ctx: Ctx): GroupVisitor[Option[A]] =
      ctx.reporter.error("expected a scalar, found a setting group")
      NoneGroupVisitor

    def visitArray(ctx: Ctx): ArrayVisitor[Option[A]] =
      ctx.reporter.error("expected a scalar, found an array")
      NoneArrayVisitor
    def visitString(ctx: Ctx, str: String): Option[A] =
      areader.read(str) match
        case Reader.Success(s) => Some(s)
        case Reader.Error(msg) =>
          ctx.reporter.error(msg)
          None

