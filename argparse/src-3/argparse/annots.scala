package argparse

/** Parameter annotation to change behavior of the annotation API. Annotate a
  * method parameter with this annotation to override some aspects of
  * macro-generated code.
  *
  * Override the name of a parameter. The name will be used as-is. Note that in
  * particular this means that you need to specify leading dashes for named
  * parameters.
  */
case class name(name: String) extends annotation.StaticAnnotation

/** Parameter annotation to change behavior of the annotation API. Annotate a
  * method parameter with this annotation to override some aspects of
  * macro-generated code.
  *
  * Add aliases to the name of the parameter. This can be especially helpful for
  * defining short names, e.g. `@arg("-s") server: String = ...`.
  */
case class alias(aliases: String*) extends annotation.StaticAnnotation

/** Parameter annotation to change behavior of the annotation API. Annotate a
  * method parameter with this annotation to override some aspects of
  * macro-generated code.
  *
  * Set an environment variable from which a parameter will be read if not found
  * on the command line.
  */
case class env(env: String) extends annotation.StaticAnnotation
