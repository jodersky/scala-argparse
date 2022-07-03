package argparse
package core

trait TypesApi {

  /** A typeclass that defines how to convert a string from a single command line
    * argument to a given type.
    */
  @annotation.implicitNotFound(
    "No argparse.Reader[${A}] found. A reader is required to parse a command line argument from a string to a ${A}. " +
    "Please define an argparse.Reader[${A}]."
  )
  trait Reader[A] {

    /** Either convert the string to A or return a failure message.
      *
      * Note that throwing an exception from a reader will cause the parser
      * to crash, leading to a horrible user experience.
      */
    def read(a: String): Reader.Result[A]

    /** The human-friendly name of this reader's argument type. */
    def typeName: String

    /** Compute available shell completions starting with a given string. This
      * is used by interactive bash completion, where the user program is
      * responsible for generating completions.
      */
    def interactiveCompleter: String => Seq[String] = _ => Seq.empty

    /** A completer for bash. This is used by standalone bash completion, where
      * a bash script generates completion, without the involvement of the the
      * user program.
      *
      * If your program is implemented with Scala on the JVM, the startup time
      * is considerable and hence standalone completion should be preferred for
      * a snappy user experience.
      */
    def standaloneCompleter: BashCompleter = BashCompleter.Empty
  }

  object Reader {
    sealed trait Result[+A]
    case class Success[A](value: A) extends Result[A]
    case class Error(message: String) extends Result[Nothing]
  }

  implicit val StringReader: Reader[String] = new Reader[String] {
    def read(a: String) = Reader.Success(a)
    def typeName: String = "string"
  }

}
