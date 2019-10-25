(ns grape.core
  (:require [instaparse.core :as insta]
            [parcera.core :as parcera]))

#_
(defn- tree-node?
  [x]
  (and (vector? x)
       (>= 2 (count x))
       (keyword? (first x))))

(defn parse-string
  "Parse a string as an AST."
  [code]
  (parcera/clojure code))

(defn parse-string-with-meta
  "Equivalent of parse-string that adds position metadata to each AST node."
  [code]
  ;; https://github.com/Engelberg/instaparse#line-and-column-information
  (insta/add-line-and-column-info-to-metadata
    code
    (parse-string code)))

(defn match-subtree
  "Match a tree given a subtree pattern. Return the first matching subtree, if
   any."
  [tree subtree-pattern]
  ;; first implementation: one match + no wildcard
  ;; TODO see e.g. http://www.iaeng.org/publication/IMECS2011/IMECS2011_pp206-211.pdf
  )
