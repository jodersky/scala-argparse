package cmdr.macros

object util {

  /** `thisIsSnakeCase => this_is_snake_case` */
  def snakify(camelCase: String): String = {
    val snake = new StringBuilder
    var prevIsLower = false
    for (c <- camelCase) {
      if (prevIsLower && c.isUpper) {
        snake += '_'
      }
      snake += c.toLower
      prevIsLower = c.isLower
    }
    snake.result()
  }

  /** `thisIsKebabCase => this-is-kebab-case` */
  def kebabify(camelCase: String): String = {
    val kebab = new StringBuilder
    var prevIsLower = false
    for (c <- camelCase) {
      if (prevIsLower && c.isUpper) {
        kebab += '-'
      }
      kebab += c.toLower
      prevIsLower = c.isLower
    }
    kebab.result()
  }

}
