# Patterns

Patterns consist of literals and/or wildcards.

They are matched recursively: `[$]` matches `[[1]]` twice; once on `[[1]]` and
once on `[1]`.

## Literals

literals are matched, well, literally. Like in a regular expression, they match
their own representation.

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

| Pattern | Description | Example expressions |
|:---     | :---        | :---                |
| `$backtick` | a backtick-ed expression | `` `foo `` |
| `$character` | a character | `\space`, `\a` |
| `$conditional-splicing` | a reader splicing conditional | `#?@(:cljs nil)` |
| `$conditional` | a reader conditional | `#?(:cljs nil)` |
| `$deref` | a dereferenced expression using `@` | `@foo` |
| `$function` | an anonymous function | `#(do %)` |
| `$list` | a list | `(f 2 3)` |
| `$macro-keyword` | a keyword defined with `::` | `::foo` |
| `$map` | a map | `{}` |
| `$metadata` | a metadata-tagged expression | `^:private my-sym` |
| `$number` | a number (int or float) | `42`, `3.14` |
| `$quote` | a quoted expression | `'foo` |
| `$regex` | a regular expression | `#"foo"` |
| `$set` | a set | `#{}` |
| `$simple-keyword` | a keyword | `:foo` |
| `$string` | a string | `"foo"` |
| `$symbol` | a symbol | `foo`, `foo/bar`, `clojure.string/trim` |
| `$symbolic` | a special symbol such as `##NaN` or `##Inf` | `##Inf` |
| `$unquote-splicing` | a splice-unquoted expression | `~@foo` |
| `$unquote` | an unquoted expression | `~foo` |
| `$var-quote` | | `#'foo` |
| `$vector` | a vector | `[]` |

This list is based on [Parcera’s grammar][pg].

[pg]: https://github.com/carocad/parcera/blob/d6b28b1058ef2af447a9452f96c7b6053e59f613/src/parcera/core.cljc#L26

Note: when matching collections you can have a better control over their
content by using litterals: `$vector` matches any vector, while `[]` matches
empty vectors and `[$ $ $]` matches vectors of exactly 3 elements. These can of
course be combined: `[$ $number $ $regex]` matches all 4-elements vectors where
the second one is a number and the fourth and last one a regular expression.
