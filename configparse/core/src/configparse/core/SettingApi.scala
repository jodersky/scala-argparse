package configparse.core

object SettingApi:
  /** `thisIsCamelCase => this_is_snake_case` */
  private def snakify(camelCase: String): String =
    val snake = new StringBuilder
    var prevIsLower = false
    for c <- camelCase do
      if prevIsLower && c.isUpper then
        snake += '_'
      snake += c.toLower
      prevIsLower = c.isLower
    snake.result()

trait SettingApi:
  api =>

  trait SettingReader[A] extends Visitor[Option[A]]

  /** Convert a scala variable name to a setting name. */
  def settingKeyName(scalaName: String): String = SettingApi.snakify(scalaName)

  def setting[A](default: => A)(
    using reader: SettingReader[A],
    doc: DocComment
  ): Setting[A] =
    Setting(Some(() => default), doc.paragraphs.mkString("\n"), reader)

  class Setting[A](
    val default: Option[() => A],
    val description: String,
    val reader: SettingReader[A],
  ):
    // a side-effecting visitor which sets this setting
    val visitor: Visitor[Unit] = new Visitor[Unit]:
      def visitString(ctx: Ctx, str: String): Unit =
        val pos = ctx.pos
        reader.visitString(ctx, str) match
          case None =>
          case Some(a) => set(a)(using pos)
      def visitGroup(ctx: Ctx): GroupVisitor[Unit] = new GroupVisitor[Unit]:
        val v = reader.visitGroup(ctx)
        def visitKey(ctx: Ctx, key: String): Unit = v.visitKey(ctx, key)
        def subVisitor(): Visitor[_] = v.subVisitor()
        def visitValue(ctx: Ctx, value: Any): Unit =
          v.visitValue(ctx, value)
        def visitEnd(): Unit =
          v.visitEnd() match
            case None =>
            case Some(a) =>  set(a)(using ctx.pos)

      def visitArray(ctx: Ctx): ArrayVisitor[Unit] = new ArrayVisitor[Unit]:
        val v = reader.visitArray(ctx)
        def visitIndex(ctx: Ctx, idx: Int): Unit = v.visitIndex(ctx, idx)
        def subVisitor(): Visitor[_] = v.subVisitor()
        def visitValue(ctx: Ctx, value: Any): Unit = v.visitValue(ctx, value)

        def visitEnd(): Unit =
          v.visitEnd() match
            case None =>
            case Some(a) =>  set(a)(using ctx.pos)

    private var empty: A = _
    private var _value: A = _
    private var _path: Seq[String] = Seq()
    private var _isSet: Boolean = false
    private var _pos: Position = Position.NoPosition

    /** Has this actually been set, or are we using a default value? */
    def isSet: Boolean = _isSet

    def pos = _pos

    /** The current value of this setting */
    def value: A =
      if _isSet then _value
      else if default.isDefined then default.get()
      else sys.error("not defined")

    def set(value: A)(using pos: Position): this.type =
      _value = value
      _isSet = true
      _pos = pos
      this

    def setPath(path: Seq[String]): this.type =
      _path = path
      this

    def reset(): this.type =
      _value = empty
      _isSet = false
      _pos = Position.NoPosition
      this

    override def toString: String =
      if _isSet then
        s"setting '${_path.mkString(".")}' set to '$value' at ${_pos.pretty}"
      else
        s"setting '${_path.mkString(".")}' unset"
  end Setting

  trait SettingRoot[S]:
    def tree(s: S): SettingTree

  object SettingRoot:
    inline def derived[S]: SettingRoot[S] = ${Macros.derivedImpl[api.type, S]('api)}


  import collection.mutable

  type FileReader = (os.Path, MutableCtx, SettingTree) => Unit
  private val fileReaders = collection.mutable.LinkedHashMap.empty[String, FileReader]

  /** Extension to reader */
  def registerSettingExtension(
    ext: String,
    accept: (os.Path, MutableCtx, SettingTree) => Unit
  ) = fileReaders += ext -> accept


  def read[S](
    settings: S,
    sources: Seq[os.Path] = Seq(),
    envPrefix: String = "",
    env: Map[String, String] = Map(),
    check: Boolean = true,
    err: java.io.PrintStream = System.err
  )(using root: SettingRoot[S]): Boolean =
    val tree = root.tree(settings)

    var errors: Int = 0
    val mctx = new MutableCtx:
      var pos: Position = Position.NoPosition
      var text: String = ""
      def comment: String = ???

      def report(message: String) =
        err.println(s"${pos.pretty}: $message")
        pos match
          case Position.File(path, line, col) if text != "" =>
            err.println(text)
            err.println(" " * (col - 1) + "^")
          case _ =>

      val reporter = new Reporter:
        def error(message: String): Unit =
          report(message)
          errors += 1
        def warn(message: String): Unit =
          report(message)

    for
      source <- sources
      candidates = if os.isDir(source) then os.list(source) else Seq(source)
      file <- candidates.sorted
      if !os.isDir(file)
    do
      fileReaders.get(file.ext) match
        case None =>
          err.println(s"$file: files ending in '${file.ext}' aren't supported")
          errors += 1
        case Some(reader) =>
          reader(file, mctx, tree)

    tree.traverseNamed{ (path, setting) =>
      val key = envPrefix + path.map(_.toUpperCase()).mkString("_")
      env.get(key) match
        case None =>
        case Some(v) =>
          setting.visitor.visitString(mctx, v)
    }

    if errors == 0 && check then
      tree.traverseNamed{ (path, s) =>
        if !s.default.isDefined && !s.isSet then err.println(s"required setting '${path.mkString(".")}'")
      }
    errors == 0
  end read

end SettingApi
