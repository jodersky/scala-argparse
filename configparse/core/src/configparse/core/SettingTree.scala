package configparse.core

/** A tree of program-declared settings and groups.
  *
  * An instance of this class is typically generated via a macro.
  */
case class SettingTree(
  settings: Map[String, SettingApi#Setting[_]],
  groups: Map[String, SettingTree]
):
  val visitor: Visitor[Unit] = new Visitor[Unit]:
    override def visitString(ctx: Ctx, str: String): Unit =
      ctx.reporter.error("expected a setting group, found a string")
    override def visitGroup(ctx: Ctx): GroupVisitor[Unit] = groupVisitor
    override def visitArray(ctx: Ctx): ArrayVisitor[Unit] =
      ctx.reporter.error("expected a setting group, found an array")
      NoopArrayVisitor

  val groupVisitor: GroupVisitor[Unit] = new GroupVisitor[Unit]:
    private var child: Visitor[Unit] = NoopVisitor

    override def visitKey(ctx: Ctx, key: String): Unit =
      settings.get(key) match
        case Some(s) => child = s.visitor
        case None =>
          groups.get(key) match
            case Some(st) => child = st.visitor
            case None => child = NoopVisitor
    override def subVisitor(): Visitor[Unit] = child
    override def visitValue(ctx: Ctx, value: Any): Unit = ()
    override def visitEnd(): Unit = ()

  def traverse(fn: SettingApi#Setting[_] => Unit): Unit =
    for (_, s) <- settings do fn(s)
    for (_, c) <- groups do c.traverse(fn)

  def traverseNamedPath(path: Seq[String])(fn: (Seq[String], SettingApi#Setting[_]) => Unit): Unit =
    for (n, s) <- settings do fn(path ++ Seq(n), s)
    for (n, c) <- groups do c.traverseNamedPath(path ++ Seq(n))(fn)

  def traverseNamed(fn: (Seq[String], SettingApi#Setting[_]) => Unit): Unit =
    traverseNamedPath(Seq())(fn)

end SettingTree
