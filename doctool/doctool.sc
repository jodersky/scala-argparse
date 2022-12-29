import mill._

trait DocsModule extends mill.Module {
  private implicit def relrw(implicit str: upickle.default.ReadWriter[String]):
    upickle.default.ReadWriter[os.SubPath] = str.bimap(p => p.toString, s => os.SubPath(s))

  def docSources: T[Seq[PathRef]] = T.sources(millSourcePath)

  def sidebar: T[PathRef] = T.source(millSourcePath / "_Sidebar.md")
  def repoUrl = T{"https://github.com/jodersky/scala-argparse"}
  def editRootUrl = T{"https://github.com/jodersky/scala-argparse/blob/master/docs"}

  def docSourceFiles: T[Seq[(os.SubPath, PathRef)]] = T {
    for {
      root <- docSources()
      file <- os.walk(root.path)
      if !os.isDir(file)
    } yield (file.subRelativeTo(root.path), PathRef(file, true))
  }

  def markdownFiles = T{
    docSourceFiles().filter(_._2.path.ext == "md")
  }

  def docVersion: T[String]

  private val IncludeRegex = """\$include:(.*)\$""".r

  // run the preprocessor to include source snippets
  def preprocess: T[Seq[(os.SubPath, PathRef)]] = T {
    var hasErrors = false
    val replaced = for ((rel, pathref) <- markdownFiles()) yield {
      val replaced = IncludeRegex.replaceAllIn(
        os.read(pathref.path).replaceAll("""\$version\$""", docVersion()),
        m =>
          try {
            val file = m.group(1)
            val content = os.read(os.Path(file, os.pwd))
            java.util.regex.Matcher.quoteReplacement(content)
          } catch {
            case ex: Exception =>
              T.log.error(
                pathref.path.toString + " " + m.group(0) + ": " + ex.toString)
              hasErrors = true
              ""
          }
      )
      os.write(T.dest / rel, replaced, createFolders = true)
      (rel, PathRef(T.dest / rel))
    }
    if (hasErrors) {
      mill.api.Result.Failure("Failed to preprocess some markdown")
    } else {
      mill.api.Result.Success(replaced)
    }
  }

  def luaFilters = T.sources(os.pwd / "doctool" / "filters")
  def template: T[PathRef] = T.source(os.pwd / "doctool" / "page.template.html")

  def mainCss = T.source(os.pwd / "doctool" / "bootstrap.min.css")
  def mainJs = T.source(os.pwd / "doctool" / "bootstrap.bundle.min.js")

  def sidebarFilters = T.sources(os.pwd / "doctool" / "sidebarfilters")

  def html = T {
    val css = T.dest / "bootstrap.min.css"
    os.copy(mainCss().path, css)

    val js = T.dest / "bootstrap.bundle.min.js"
    os.copy(mainJs().path, js)

    for ((rel, pathref) <- preprocess()) {
      os.makeDir.all(T.dest / rel / os.up)
      val rootPath = T.dest.relativeTo((T.dest / rel / os.up))
      val root = if (rootPath == os.RelPath("")) "." else rootPath.toString
      println(root)
      val sidebarContent = os.proc(
        "pandoc",
        sidebar().path,
        "-t", "html5",
        "--metadata", s"prefix:${root}",
        sidebarFilters().flatMap(p => os.walk(p.path)).map(f => s"--lua-filter=${f}")
      ).call().out.text()

      val command = os.proc(
        "pandoc",
        pathref.path,
        "--to", "html5",
        "--output", T.dest / rel / os.up / (rel.baseName + ".html"),
        "--template", template().path,
        "--css", css.relativeTo(T.dest / rel / os.up),
        "--toc",
        "--highlight-style", "pygments",
        "--variable", s"nav:${sidebarContent}",
        "--variable", s"js:${js.relativeTo(T.dest / rel / os.up)}",
        "--variable", s"repo-url:${repoUrl()}",
        "--variable", s"edit-url:${editRootUrl()}/${rel}",
        luaFilters().flatMap(p => os.walk(p.path)).map(f => s"--lua-filter=${f}")
      )
      T.log.info(command.command.flatMap(_.value).mkString(" "))
      command.call()
    }

    PathRef(T.dest)
  }

}
