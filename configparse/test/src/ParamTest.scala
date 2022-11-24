import utest.*

object ParamTest extends TestSuite:
  object ap extends configparse.Api:
    // override so that we don't exit the JVM on failure
    override protected def exit(code: Int): Nothing = sys.error("error: " + code)

  class Cfg derives ap.SettingRoot:

    val x = ap.setting[String]("a")

    object nested:
      val y = ap.setting[String]("a")

      object deep:
        val z = ap.setting[Int](42)

    val col = ap.setting[Seq[String]](Seq())

    class Addr:
      val host = ap.setting[String]("")
      val port = ap.setting[Int](0)

    object listen extends Addr
    object advertise extends Addr

  @ap.main()
  def foo(config: Cfg = Cfg()) =
    println(config.x)
    println(config.nested.y)
    println(config.nested.deep.z)
    println(config.col)

    println(config.listen.host)
    println(config.listen.port)
    println(config.advertise.host)
    println(config.advertise.port)

  def entry(args: Array[String]) = ap.dispatch(this, args)

  val tests = Tests {
    test("entrypoint") {
      val yaml =
        s"""|x: 3
            |
            |nested:
            |  y: "hello"
            |  deep:
            |    z: 1000
            |
            |col:
            | - a
            | - b
            | - c
            | - f
            |
            |listen:
            |  host: hello
            |  port: 2
            |
            |advertise:
            |  host: world
            |  port: 3
            |
            |
            |""".stripMargin
      val file = os.temp(yaml, suffix=".yaml")
      entry(Array("--config", file.toString))
    }
  }
