package argparse

object SettingsParser {
  import scala.quoted.{Quotes, Type, Expr}

  def settingsImpl[A: Type](
    parser: Expr[ArgumentParser],
    prefix: Expr[String]
  )(using Quotes): Expr[() => A] = {
    Macros().caseClassParser[A](parser, '{$prefix + "."})
  }
}

trait SettingsParser { self: ArgumentParser =>

  /** EXPERIMENTAL
    *
    * Add parameter definitions for all vars in a class and nested objects.
    *
    * Variables will be set when a corresponding argument is encountered on the
    * commandline. The rules are as follows:
    *
    * - Every variable will be assigned a named parameter (starting with '--')
    *
    * - The named parameter is optional. If it is not encountered on the command
    *   line, the variable's default value will be used (since this method
    *   requires a class *instance*, variables cannot be abstract).
    *
    * - For every variable of type A, an implicit Reader[A] must be available.
    *
    * - Boolean variables are parsed as flags.
    *
    * Example:
    *
    * ```
    * class Settings {
    *   var opt1: String = "default1"
    *   var opt2: Int = 42
    *   var enableSystemA: Boolean = false
    *
    *   object http {
    *     var host: String = "localhost"
    *     var port: Int = 2020
    *   }
    *
    *   object monitoring {
    *     var host: String = "localhost"
    *     var port: Int = 3030
    *   }
    * }
    *
    * def main(args: Array[String]): Unit = {
    *   val parser = argparse.ArgumentParser()
    *
    *   val settings = parser.mutableSettings(Settings())
    *
    *   println("before")
    *   println(settings) // all default values
    *
    *   parser.parse(Array(
    *     "--opt1", "value1", "--enable-system-a", "--monitoring.port", "3031"
    *   ))
    *
    *   println("after")
    *   println(settings) // opt1, enableSystemA and monitoring.port have been changed
    * }
    * ```
    */
  inline def mutableSettings[A](a: A): A = ${MutableSettingsParser.impl[A]('this, 'a)}

  /** EXPERIMENTAL
    *
    * Add arguments for case-class fields (recursively).
    */
  inline def settings[A](name: String): () => A =
    ${SettingsParser.settingsImpl[A]('this, 'name)}

}
