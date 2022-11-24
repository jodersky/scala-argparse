import utest._

import configparse.default as ap

object SettingsTest extends TestSuite:

  object cfg derives ap.SettingRoot:
    /** An example setting */
    val s = ap.setting[String]("a")

    object nested:
      val x = ap.setting[String]("b")

      object deep:
        val y = ap.setting[Int](42)
  end cfg

  class ComponentA:
    val x = ap.setting[String]("a")
  class ComponentB:
    val y = ap.setting[String]("a")
  class Composed derives ap.SettingRoot:
    object a extends ComponentA
    object a1 extends ComponentA
    object b extends ComponentB

  trait Component1:
    val x = ap.setting[String]("a")
  trait Component2:
    val y = ap.setting[String]("a")
  class Inherited extends Component1 with Component2 derives ap.SettingRoot

  val tests = Tests {
    test("basic") {
      assert(cfg.s.value == "a")
      assert(cfg.nested.x.value == "b")
      assert(cfg.nested.deep.y.value == 42)

      assert(ap.read(cfg) == true)

      assert(cfg.s.value == "a")
      assert(cfg.nested.x.value == "b")
      assert(cfg.nested.deep.y.value == 42)

      assert(ap.read(cfg, env = Map("FOO" -> "2", "CFG_NESTED_DEEP_Y" -> "1000"), envPrefix = "CFG_") == true)

      assert(cfg.nested.deep.y.value == 1000)
    }
    test("composition") {
      val s = Composed()
      assert(s.a.x.value == "a")
      assert(s.a1.x.value == "a")
      assert(s.b.y.value == "a")
      assert(ap.read(s, env = Map("A_X" -> "b", "A1_X" -> "b", "B_Y" -> "b"), envPrefix = "") == true)
      assert(s.a.x.value == "b")
      assert(s.a1.x.value == "b")
      assert(s.b.y.value == "b")
    }
    test("inheritance") {
      val s = Inherited()
      assert(s.x.value == "a")
      assert(s.y.value == "a")
      assert(ap.read(s, env = Map("X" -> "b", "Y" -> "b"), envPrefix = "") == true)
      assert(s.x.value == "b")
      assert(s.y.value == "b")
    }
  }
