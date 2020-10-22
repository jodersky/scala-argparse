package cmdr

/** An arg represents a handle to a parameter's value. */
trait Arg[A] {

  @deprecated("use apply() instead", "0.2.1")
  def get: A = apply()

  /** Get the value of this argument.
    *
    * This value will only be abailable after an argument has been parsed. It
    * will throw otherwise.
    */
  def apply(): A
}
