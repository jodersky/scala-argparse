package configparse

import yamlesque.Ctx as Yctx
import configparse.core.MutableCtx

trait ContextHelper:
  def mctx: MutableCtx

  def mkCtx(ctx: Yctx) =
    mctx.pos = configparse.core.Position.File(
      ctx.pos.file, ctx.pos.line, ctx.pos.col
    )
    mctx.text = ctx.line
    mctx

class YamlVisitor[+A](val mctx: MutableCtx, v: configparse.core.Visitor[A]) extends yamlesque.Visitor[A] with ContextHelper:
  def visitEmpty(ctx: Yctx): A =
    v.visitString(mctx, "")
  def visitString(ctx: Yctx, text: CharSequence): A =
    v.visitString(mkCtx(ctx), text.toString)
  def visitBlockStringFolded(ctx: Yctx, text: CharSequence): A = visitString(ctx, text)
  def visitQuotedString(ctx: Yctx, text: CharSequence): A = visitString(ctx, text)
  def visitBlockStringLiteral(ctx: Yctx, text: CharSequence): A = visitString(ctx, text)

  def visitArray(ctx: Yctx): yamlesque.ArrayVisitor[A] = YamlArrayVisitor(mctx, v.visitArray(mkCtx(ctx)))
  def visitObject(ctx: Yctx) = YamlGroupVisitor(mctx, v.visitGroup(mkCtx(ctx)))

class YamlGroupVisitor[+A](val mctx: MutableCtx, v: configparse.core.GroupVisitor[A]) extends yamlesque.ObjectVisitor[A] with ContextHelper:
  def visitKey(ctx: Yctx, key: String): Unit =
    v.visitKey(mkCtx(ctx), key)
  def subVisitor() = YamlVisitor(mctx, v.subVisitor())
  def visitValue(ctx: Yctx, value: Any): Unit =
    v.visitValue(mkCtx(ctx), value)
  def visitEnd(): A = v.visitEnd()

class YamlArrayVisitor[+A](val mctx: MutableCtx, v: configparse.core.ArrayVisitor[A]) extends yamlesque.ArrayVisitor[A] with ContextHelper:
  def visitIndex(ctx: Yctx, idx: Int): Unit =
    v.visitIndex(mkCtx(ctx), idx)
  def subVisitor() = YamlVisitor(mctx, v.subVisitor())
  def visitValue(ctx: Yctx, value: Any): Unit =
    v.visitValue(mkCtx(ctx), value)
  def visitEnd(): A = v.visitEnd()

trait YamlesqueCompat extends argparse.core.Api with configparse.core.SettingApi:

  val yamlReader: FileReader = (file: os.Path, mctx: MutableCtx, tree: configparse.core.SettingTree) =>
    val s: java.io.InputStream = os.read.inputStream(file)
    try
      yamlesque.Parser(s, file.toString).parseValue(0, YamlVisitor(mctx, tree.visitor))
    catch
      case err: yamlesque.ParseException =>
        mctx.pos = configparse.core.Position.File(err.position.file, err.position.line, err.position.col)
        mctx.text = err.line
        mctx.reporter.error("invalid YAML: " + err.message)
    finally
      s.close()

  registerSettingExtension("yaml", yamlReader)
  registerSettingExtension("yml", yamlReader)

