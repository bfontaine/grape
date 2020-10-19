# Grape Changelog

## 0.3.0 (unreleased)

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

## 0.2.0 (2019/11/14)

* Add typed expression wildcards

## 0.1.0 (2019/10/27)

Initial release.
