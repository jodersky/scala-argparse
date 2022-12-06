package configparse.core

enum Position:

  /** Defined in code. */
  case Code(
    path: String,
    line: Int,
    col: Int
  )

  /** Set from the command line. */
  case CommandLine(param: String, arg: String)

  /** Set via an environment variable. */
  case Env(name: String)

  /** Read from a file. */
  case File(path: String, line: Int, col: Int)

  /** No position available, or unknown. */
  case NoPosition

  def pretty: String = this match
    case Code(path, line, col) => s"(code) $path:$line:$col"
    case File(path, line, col) => s"$path:$line:$col"
    case CommandLine(param, arg) => s"command line argument $param=$arg"
    case Env(name) => s"env var $name"
    case NoPosition => "<no position>"

object Position:
  inline given Position = here
  inline def here: Position = ${hereImpl}

  import scala.quoted.*
  def hereImpl(using qctx: Quotes): Expr[Position] =
    import qctx.reflect.{Position => _, *}
    val sym = Symbol.spliceOwner.owner
    sym.pos match
      case None =>
        report.warning(s"no position available for $sym")
        '{
          Position.NoPosition
        }
      case Some(pos) if pos.sourceFile.jpath == null =>
        '{
          Position.Code(
            "<virtual>",
            ${Expr(pos.startLine)},
            ${Expr(pos.startColumn)}
          )
        }
      case Some(pos) =>
        '{
          Position.Code(
            ${Expr(pos.sourceFile.jpath.getFileName().toString)},
            ${Expr(pos.startLine)},
            ${Expr(pos.startColumn)}
          )
        }

end Position
