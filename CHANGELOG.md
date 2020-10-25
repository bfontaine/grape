# Grape Changelog

## Unreleased
### CLI
* Matches are now prefixed by line numbers
* Add `-N`/`--no-line-numbers` to hide line numbers
* Read from `stdin` if no path is given
* Accept `-` as a special path to read from `stdin`. If `-` is used multiple times, only the first one is effective.

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
