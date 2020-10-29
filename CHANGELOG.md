# Grape Changelog

## 0.6.0 (2020/10/29)
* Add support for typed multi-expressions wildcards such as `$keyword&` and `$number$` (#2)
* Rewrite a significant part of the internal code (`grape.impl.*`) to facilitate future evolutions

## 0.5.1 (2020/10/27)
### CLI
* Fix `--line-numbers none`

## 0.5.0 (2020/10/27)

### Library
* `grape.core/unparse-code` now accept an optional map of options. Only `:inline?` is supported for now; it forces code
  to fit on one line by removing newlines and comments and compacting whitespaces.

### CLI
* Fix `--version` that was failing with an `IllegalArgumentException` (#1)
* The first line of each match is now prefixed by its line number
* Matches are now followed by a newline
* Read from `stdin` if no path is given
* Accept `-` as a special path to read from `stdin`. If `-` is used multiple times, only the first one is effective.
* Add `--line-numbers first|all|none` to control how line numbers are shown
* Add `-N`/`--no-line-numbers` as an alias to `--line-numbers none`
* Add `-n`/`--all-line-numbers` as an alias to `--line-numbers all`
* Add `--no-trailing-newlines` to remove the (new) trailing newline after each match
* Add `--inline` to always show matches on a single line

## 0.4.0 (2020/10/24)

* Fix a bug where `$` was matching whitespaces

### Library
* Add `grape.core/count-subtrees` and `grape.core/count-codes`

### CLI
* Show matching filenames when `grape` is run on multiple files
* Add `-v`/`--version` to the command-line
* Add `-c`/`--count` to show the total matches count
* Add `-F`/`--no-filenames` to hide the matching filenames
* Add `-u`/`--unindent` to un-indent the matches
* Remove `-r`/`--recursive`: it’s always recursive now

### Internals
* Extract code from `grape.core` into `grape.impl.models`, `grape.impl.match` and `grape.impl.parsing`
* Add more tests

## 0.3.0 (2020/10/19)

This release bumps Parsera. It’s now based on Antlr4 rather than Instaparse. This changes a few things:

The match metadata’s format changes:
```clojure
;; Before
{:start-column 1, :end-column 10
 :start-line   1, :end-line 1
 :start-index  0, :end-index 9}

;; After
{:start {:row 1 :column 1}
 :end   {:row 0 :column 9}}
```

Some typed wildcards change: `$simple-keyword` is now `$keyword` and `$function` is now `$fn`.

This release also includes a first GraalVM-based standalone binary. Thanks to both GraalVM and the new Parcera
implementation, `grape` is tremendously faster: on my machine, searching for `(map $ $ $)` in grape’s own
source code takes ~2s with the v0.2.0 and ~0.06s with the v0.3.0.

## 0.2.0 (2019/11/14)

* Add typed expression wildcards

## 0.1.0 (2019/10/27)

Initial release.
