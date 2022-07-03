package argparse

/** The future value of an argument.
  *
  * You can obtain an instance of this class through the various `param()`
  * methods of ArgumentParser.
  *
  * Once (string) arguments have been parsed by the ArgumentParser, the
  * `.value()` method in this class will become available.
  */
class Argument[A](name: String) extends (() => A) {
  private var _isSet = false
  private var _value: A = _

  private[argparse] def set(value: A) = {
    _isSet = true
    _value = value
  }

  /** Call this method to retrieve the argument's actual value. */
  def value: A = if (_isSet) _value else {
    throw new NoSuchElementException(
      s"The argument '$name' is not yet available. ArgumentParser.parse(args) must " +
      "be called before this value can be accessed."
    )
  }

  @deprecated("use .value instead", "0.15.2")
  def apply() = value

}
