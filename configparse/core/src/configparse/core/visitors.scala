package configparse.core

trait Reporter:
  def error(message: String): Unit
  def warn(message: String): Unit

trait Ctx:
  def pos: Position
  def reporter: Reporter

trait MutableCtx extends Ctx:
  def pos_=(p: Position): Unit

  def text: String
  def text_=(t: String): Unit

trait Visitor[+A]:
  def visitString(ctx: Ctx, str: String): A
  def visitGroup(ctx: Ctx): GroupVisitor[A]
  def visitArray(ctx: Ctx): ArrayVisitor[A]

trait GroupVisitor[+A]:

  def visitKey(ctx: Ctx, key: String): Unit
  def subVisitor(): Visitor[_]
  def visitValue(ctx: Ctx, value: Any): Unit
  def visitEnd(): A

trait ArrayVisitor[+A]:

  def visitIndex(ctx: Ctx, idx: Int): Unit
  def subVisitor(): Visitor[_]
  def visitValue(ctx: Ctx, value: Any): Unit
  def visitEnd(): A

object NoopVisitor extends Visitor[Unit]:
  override def visitString(ctx: Ctx, str: String): Unit = ()
  override def visitGroup(ctx: Ctx): GroupVisitor[Unit] = NoopGroupVisitor
  override def visitArray(ctx: Ctx): ArrayVisitor[Unit] = NoopArrayVisitor

object NoopGroupVisitor extends GroupVisitor[Unit]:

  override def visitKey(ctx: Ctx, key: String): Unit = ()
  override def subVisitor(): Visitor[_] = NoopVisitor
  override def visitValue(ctx: Ctx, value: Any): Unit = ()
  override def visitEnd(): Unit = ()

object NoopArrayVisitor extends ArrayVisitor[Unit]:
  override def visitIndex(ctx: Ctx, idx: Int): Unit = ()
  override def subVisitor(): Visitor[?] = NoopVisitor
  override def visitValue(ctx: Ctx, value: Any): Unit = ()
  override def visitEnd(): Unit = ()

object NoneGroupVisitor extends GroupVisitor[Option[Nothing]]:
  def visitKey(ctx: Ctx, key: String): Unit = ()
  def subVisitor(): Visitor[?] = NoopVisitor
  def visitValue(ctx: Ctx, value: Any): Unit = ()
  def visitEnd(): None.type = None

object NoneArrayVisitor extends ArrayVisitor[Option[Nothing]]:
  def visitIndex(ctx: Ctx, idx: Int): Unit = ()
  def subVisitor(): Visitor[?] = NoopVisitor
  def visitValue(ctx: Ctx, value: Any): Unit = ()
  def visitEnd(): None.type = None
