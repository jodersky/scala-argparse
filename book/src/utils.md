# Utilities

Argparse has a few utility classes that can help with some common
application-related tasks.

## Terminal Properties

The [`argparse.term`](./javadoc/api/argparse/term$.html) helper contains methods
to retrieve terminal properties, such as number of rows and columns.

## Standard File Paths

You can use [`argparse.userdirs`](./javadoc/api/argparse/userdirs$.html) to
access standard directories for configuration, state or data of user
applications. This utility is based on the [XDG Base Directory
Specification](https://specifications.freedesktop.org/basedir-spec/basedir-spec-latest.html),
with some adaptations made for macOS. It is recomended to use this helper
instead of hardcoding directories or creating a new folder in the user's home
directory.
