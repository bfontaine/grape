# Patterns

Patterns consist of literals and/or wildcards.

They are matched recursively: `[$]` matches `[[1]]` twice; once on `[[1]]` and once on `[1]`.

## Literals

literals are matched, well, literally. Like in a regular expression, they match their own representation:
the pattern `42` matches the code `42`.

## Wildcards
### `$`

`$` matches any expression.

Examples:
* `$` matches literal like `42` but also more complex expressions like `(do (println "foo") (update my-map :foo inc))`.
* `[$]` matches vectors of one single element.

### `$&`

`$&` matches any number of expressions, including zero.

There can be only one `$&` per sequence of expressions (contiguous `$&`s are
equivalent to one `$&`).

### `$type`

`$type` is an equivalent to `$` with a constraint on the literal type. It
matches any expression with the given type. See the table below for a
description of the available types:

| Pattern | Description | Example expressions |
|:---     | :---        | :---                |
| `$backtick` | a backtick-ed expression | `` `foo `` |
| `$character` | a character | `\space`, `\a` |
| `$conditional-splicing` | a reader splicing conditional | `#?@(:cljs nil)` |
| `$conditional` | a reader conditional | `#?(:cljs nil)` |
| `$deref` | a dereferenced expression using `@` | `@foo` |
| `$fn` | an anonymous function | `#(do %)` |
| `$keyword` | a keyword | `:foo` |
| `$list` | a list | `(f 2 3)` |
| `$macro-keyword` | a keyword defined with `::` | `::foo` |
| `$map` | a map | `{}` |
| `$metadata` | a metadata-tagged expression | `^:private my-sym` |
| `$number` | a number (int or float) | `42`, `3.14` |
| `$quote` | a quoted expression | `'foo` |
| `$regex` | a regular expression | `#"foo"` |
| `$set` | a set | `#{}` |
| `$string` | a string | `"foo"` |
| `$symbol` | a symbol | `foo`, `foo/bar`, `clojure.string/trim` |
| `$symbolic` | a special symbol such as `##NaN` or `##Inf` | `##Inf` |
| `$unquote-splicing` | a splice-unquoted expression | `~@foo` |
| `$unquote` | an unquoted expression | `~foo` |
| `$var-quote` | | `#'foo` |
| `$vector` | a vector | `[]` |

This list is based on [Parcera’s grammar][pg], except that underscores are replaced with dashes.

[pg]: https://github.com/carocad/parcera/blob/83cd988e69116b67c620c099f78b693ac5e37233/src/Clojure.g4

Note: when matching collections you can have a better control over their content by using literals:
`$vector` matches any vector, while `[]` matches empty vectors and `[$ $ $]` matches vectors of exactly
3 elements. These can of course be combined: `[$ $number $ $regex]` matches all 4-elements vectors where
the second one is a number and the fourth and last one a regular expression.
