package cmdr

/** Common directories for *user* applications, as specified by the [XDG Base
  * Directory
  * Specification](https://specifications.freedesktop.org/basedir-spec/basedir-spec-latest.html),
  * with some adaptations made on macOS.
  *
  * @see dirs for conventional directories for an application of a given name,
  * including system services.
  */
object xdg {

  private val isMac = sys.props("os.name").toLowerCase.startsWith("mac")

  /** A directory relative to which user-specific data files should be written.
    *
    * Corresponds to XDG_DATA_HOME.
    */
  def dataHome: os.Path =
    sys.env
      .get("XDG_DATA_HOME")
      .map(s => os.Path(s))
      .getOrElse {
        if (isMac) {
          os.home / "Library"
        } else {
          os.home / ".local" / "share"
        }
      }

  /** A list of preference ordered directories relative to which data
    * files should be searched, in addition to [[dataHome]].
    *
    * Corresponds to XDG_DATA_DIRS.
    */
  def dataDirs: List[os.Path] =
    sys.env
      .get("XDG_DATA_DIRS")
      .getOrElse("/usr/local/share/:/usr/share/")
      .split(":")
      .map(s => os.Path(s))
      .toList

  /** A directory relative to which user-specific configuration files should be
    * written.
    *
    * Corresponds to XDG_CONFIG_HOME.
    */
  def configHome: os.Path =
    sys.env
      .get("XDG_CONFIG_HOME")
      .map(s => os.Path(s))
      .getOrElse {
        if (isMac) {
          os.home / "Library" / "Preferences"
        } else {
          os.home / ".config"
        }
      }

  /** A list of preference ordered directories relative to which configuration
    * files should be searched, in addition to [[configHome]].
    *
    * Corresponds to XDG_CONFIG_DIRS.
    */
  def configDirs: List[os.Path] =
    sys.env
      .get("XDG_CONFIG_DIRS")
      .getOrElse("/etc/xdg")
      .split(":")
      .map(s => os.Path(s))
      .toList

  /** A directory in which to read and write user-specific non-essential
    * (cached) data.
    *
    * Corresponds to XDG_CACHE_HOME.
    */
  def cacheHome: os.Path =
    sys.env
      .get("XDG_CACHE_HOME")
      .map(s => os.Path(s))
      .getOrElse {
        if (isMac) {
          os.home / "Library" / "Caches"
        } else {
          os.home / ".cache"
        }
      }

  /** A directory in which to read and write user-specific runtime files, such
    * as sockets and small temporary files.
    *
    * Corresponds to XDG_RUNTIME_DIR, with a fallback to ~/.run.
    */
  def runtime: os.Path =
    sys.env
      .get("XDG_RUNTIME_DIR")
      .map(s => os.Path(s))
      .getOrElse {
        val default = os.home / ".run"
        os.makeDir.all(default)
        System.err.println(
          s"No XDG_RUNTIME_DIR environment variable defined. Using $default instead."
        )
        default
      }

}

/** Common directories for an application of a given name.
  *
  * The directories here are implemented as a mix of the XDG Base Directory
  * Specification, macOS adaptations and a fallback to classic unix directories
  * for system applications.
  *
  * Applications are encouraged to use these directories instead of creating
  * their own hierarchies. They are particularly well-suited for use as
  * parameter defaults, for example:
  *
  * ```
  * val parser = cmdr.ArgParser()
  * val cache = parser.param[os.Path](
  *   "--cache-dir",
  *   default = cmdr.dirs("myapp").cache
  * ```
  *
  * @param name the name of the application
  * @param system use system-wide paths instead of user-specific ones
  *
  */
case class dirs(name: String, system: Boolean = false) {


  /** A list of preference ordered directories relative to which data files
    * should be searched.
    *
    * This list is guaranteed to have at least one element. The head directory
    * may be used for writing.
    */
  def data: List[os.Path] =
    if (system)
      os.root / "usr" / "share" / name :: os.root / "usr" / "local" / "share" / name :: Nil
    else xdg.dataHome / name :: xdg.dataDirs.map(_ / name)

  /** A list of preference ordered directories relative to which configuration
    * files should be searched.
    *
    * This list is guaranteed to have at least one element. The head directory
    * may be used for writing.
    */
  def config: List[os.Path] =
    if (system) os.root / "etc" / name :: Nil
    else xdg.configHome / name :: xdg.configDirs.map(_ / name)

  /** A directory in which to read and write non-essential (cached) data. */
  def cache: os.Path =
    if (system) os.root / "var" / "cache" / name
    else xdg.cacheHome / name

  /** A directory in which to read and write runtime files, such as sockets and
    * small temporary files.
    */
  def runtime: os.Path =
    if (system) os.root / "run" / name
    else xdg.runtime / name

  /** A directory for persisting application state. */
  def state: os.Path =
    if (system) os.root / "var" / "lib" / name
    else xdg.configHome / name

  /** A directory for storing log files.
    *
    * Note: in general, the logging system should not be a concern of the
    * application itself, and hence this directory should not be used. The way
    * logs are collected will vary depending on the deployment, hence the most
    * portable way to log diagnostic messages is to simply write them to stderr.
    */
  def log: os.Path =
    if (system) os.root / "var" / "log" / name
    else xdg.configHome / "log" / name

}

object dirs {

  /** Check if this application is running as a system service. */
  def guessSystem: Boolean = sys.env.get("HOME") match {
    case None => true
    case Some(home) if !home.startsWith("/home") => true
    case _ => false
  }

}
