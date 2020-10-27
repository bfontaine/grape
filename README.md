<img align="right" width="150" alt="grapes." src="./doc/grapes.png"/>

# Grape
**Grape** is a syntax-aware `grep`-like utility for Clojure code. It allows you to search for code
patterns using Clojure data structures.

## Command-line
```
$ grape [options] <pattern> [<file> ...]
```

For example, to find all usages of `map` called with three arguments in `grape`’s own code:

```
grape --unindent '(map $ $ $)' src
```
Output:
```
src/grape/impl/match.clj:
29:(map match?
        trees
        patterns)

src/grape/cli.clj:
137:(map #(when (= %1 %2) %1) prefix line-prefix)

```

Options:
* `-c`, `--count`: show the total matches count and exit.
* `-F`, `--no-filenames`: by default, `grape` shows the matching filenames when run on multiple files. This option
  disables that.
* `-u`, `--unindent`: un-indent matches.
* `--line-numbers first|all|none`: control line numbers. The default is `first`, i.e. only the first line of each match
  is prefixed by its line number. `all` shows all line numbers; `none` remove them.
* `-n`, `--all-line-numbers`: alias for `--line-numbers all`
* `-N`, `--no-line-numbers`: alias for `--line-numbers none`

Run `grape --help` to show all options.

### Install
Either get the standalone binary (faster) or a jar from the [Releases page][releases].

[releases]: https://github.com/bfontaine/grape/releases

If you have [Homebrew](https://brew.sh), you can install it like so:
```bash
brew install bfontaine/utils/grape
```

## Library

[![Clojars Project](https://img.shields.io/clojars/v/bfontaine/grape.svg)](https://clojars.org/bfontaine/grape)


```clojure
(require '[grape.core :as g])

(def my-code (slurp "myfile.clj"))

;; Find all occurrences of map called with three arguments
(g/find-codes my-code (g/pattern "(map $ $ $)"))

;; Find all occurrences of (condp = ...)
(g/find-codes my-code (g/pattern "(condp = $&)"))

;; Find all occurrences of `if` with no `else` clause
(g/find-codes my-code (g/pattern "(if $ $)"))
; => ({:match "(if …)", :meta {…}}, …)
```

Matches are map with a `:match` key that contains a string with the matching
code and a `:meta` key with line/column metadata which you can use to locate
the code in your file.

## Patterns
A pattern is any valid Clojure expression. It can contain some special symbols
that are interpreted as wildcards.

Comments, whitespaces, and discard reader macros (`#_`) are ignored when
matching.

### Wildcards
* `$`: any expression.
* `$&`: any number of expressions, including zero. `(f $&)` matches `(f)`,
  `(f 1)`, `(f 1 2)`, etc.
* `$string`, `$list`, etc: any expression of the given type.

[See the full patterns documentation](./doc/Patterns.md).

Wildcards can be combined: `#{$ $&}` matches a set with at least one element.

[parcera]: https://github.com/carocad/parcera#parcera

## License

Copyright © 2019-2020 Baptiste Fontaine

This program and the accompanying materials are made available under the terms
of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published
by the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.