import utest._, framework._

trait InteractionSuite extends TestSuite {

  private val checks = IndexedSeq.newBuilder[(String, () => Unit)]

  protected def test(name: String)(fct: => Unit): Unit = {
    checks += (name -> (() => fct))
  }

  final override def tests: Tests = {
    val ts = checks.result()
    if (ts.isEmpty) {
      Tests.apply(())
    } else {
      val names = Tree("", ts.map((name, _) => Tree(name)): _*)
      val thunks = new TestCallTree(Right(ts.map((_, method) => new TestCallTree(Left(method())))))
      Tests.apply(names, thunks)
    }
  }

  def exec(commands: Seq[String]) = {
    val builder = new ProcessBuilder(commands: _*)
      .redirectError(ProcessBuilder.Redirect.INHERIT)
      .redirectErrorStream(true)

    // builder.environment.clear()
    //for ((k, v) <- env) builder.environment.put(k, v)
    val process = builder.start()

    val reader = process.getInputStream()
    val out = new java.io.ByteArrayOutputStream
    try {
      val buffer = new Array[Byte](8192)
      var l = 0
      while
        l = reader.read(buffer)
        l != -1
      do
        out.write(buffer)

      process.waitFor()
      out.toString()
    } finally {
      process.destroy()
      reader.close()
      process.waitFor()
    }
  }

  val snippetFile = os.Path(sys.env("SNIPPET_FILE"), os.pwd)
  val snippetText = if (os.exists(snippetFile)) os.read(snippetFile) else "$"
  val snippets: Array[String] = snippetText.split("""\$\s+""").tail

  snippets.foreach { invocation =>
    val parts = invocation.split('\n')
    val command = parts.head

    test(command) {
      val expected = parts.tail.mkString("\n")
      val actual = exec(command.split("""\s+""")).trim
      assert(expected == actual)
    }
  }

}
