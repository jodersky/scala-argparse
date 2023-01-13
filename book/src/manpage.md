# Writing Man Pages

The built-in help message system is useful for quick reference, but is too terse
for thoroughly documenting command line applications. For this, I recommend that
you write a **man page** and ship it alongside every application that you
create.

I recommend that you watch the presentation ["Man, ‘splained: 40 Plus Years of
Man Page History", by Breanne
Boland](https://www.youtube.com/watch?v=_UjJMrahc8o&list=UU3Pk-8hhzME2w5BL_JvXfRg&index=16).
It goes into the reasons and best-practices of writing man pages. You can also
read the manual page's manual page (run `man man`) if you like.

## Template

Instead of writing a manual page by hand in
[troff](https://en.wikipedia.org/wiki/Troff), you can use the following markdown
template and run it through [pandoc](https://pandoc.org/).

````markdown
---
title: MY-APP
section: 1
header: User Manual
footer: App 1.0.0
date: June 2022
---

# NAME

my-app \- do something

# SYNOPSIS

**`my-app [--option1 <name>] [--option2 <name>] <arg>`**

# DESCRIPTION

An application which does something useful with `<arg>`.

The description can go into details of what the application does, and can span
multiple paragraphs.

## A subsection

You can use subsections in the description to go into finer details.

# OPTIONS

**`--option1=<string>`**
: An arbitrary string which sets some specific configuration value. Defaults to
some sane value.

**`--option2=<string>`**
: Another arbitrary string which sets some specific configuration value.
Defaults to some sane value.

# EXIT STATUS

Return 0 on success, 1 on error.

# ENVIRONMENT

`MY_APP_VARIABLE`
: Environment variable used by this application.

# FILES

`/etc/my-app.conf`
: This is an important file. Configuration values are read from this file if
they are not specified on the command-line

# EXAMPLES

**An example tells a thousand words.**

```
you can use markdown code sections
```

**Please include at least one example!**

# SEE ALSO

[A reference](https://pandoc.org/)
````

To preview the page after editing, run:

```
pandoc -s -f markdown-smart manpage.md -t man | man -l -
```

(the `-smart` is necessary here, to avoid converting '\-\-' into an em-dash)

And, once ready, save it as a man page:

```
pandoc -s -f markdown-smart manpage.md -t man > manpage.1
```

then finally ship it alongside your application.

Since it's written in markdown, you can also convert to html and make it
available online.

---
