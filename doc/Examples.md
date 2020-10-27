# Examples

## Function Arity
```clojure
;; Match all calls to f with 2 arguments
(f $ $)
;; Match all calls to f with 3 arguments
(f $ $ $)

;; Match all calls to g with 1+ arguments
(g $ $&)
```

Note: this can give wrong results if the function is in a threading macro:
```clojure
;; Matches all calls to '+ with one argument
(+ $)

(-> 42
    ;; this is matched even if the -> macro expands it into
    ;; a 2-arity call.
    (+ 1))
```

## Collections
```clojure
;; Match a set literal
$set
;; Match a map literal
$map

;; Match an empty set literal
#{}

;; Match a set literal with only one member
#{$}

;; Match a map literal with two key/value pairs
{$ $ $ $}
{$ $, $ $}

;; Match a vector literal with 2+ members
[$ $ $&]
```

## Others
```clojure
;; Match a 'do with a single body
(do $)

;; Match an 'if with no 'else' clause
(if $ $)
```