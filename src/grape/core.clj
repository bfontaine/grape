(ns grape.core
  (:require [grape.impl.models :refer [tree-node? node-children pattern?]]
            [grape.impl.match :as m :refer [match?]]
            [grape.impl.parsing :as p]))


;; -------------------
;; Parsing code & patterns
;; -------------------

(def ^{:doc "Parse code."}
  parse-code
  p/parse-code)

(def ^{:doc "Parse a piece of code as a pattern to be matched. Any expression after the
             first one is discarded as well as comments and discarded forms."}
  pattern
  m/pattern)

;; -------------------
;; High-level tree functions
;; -------------------

(defn- wrap-code-parent
  "Wrap a tree in a :code parent with the same metadata."
  [tree]
  (with-meta (list :code tree) (meta tree)))

(defn- subtrees
  [tree]
  (->> tree
       (tree-seq tree-node? node-children)
       (filter tree-node?)))

(defn- find-raw-subtrees
  [tree pattern]
  (->> tree
       subtrees
       (filter #(match? % pattern))))

(defn find-subtrees
  "Match a tree given a subtree pattern. Return a lazy sequence of subtrees."
  [tree pattern]
  (map wrap-code-parent
       (find-raw-subtrees tree pattern)))

(defn count-subtrees
  "Equivalent to (count (find-subtrees tree pattern)."
  [tree pattern]
  (-> tree
      (find-raw-subtrees pattern)
      count))

(defn find-subtree
  "Equivalent of (first (find-subtrees tree pattern))."
  [tree pattern]
  ;; first implementation: one match + one expression w/ no wildcard
  (first (find-subtrees tree pattern)))

(defn- match-meta
  "Extract the Instaparse-added location metadata from a match and return it."
  [match]
  (when-let [metadata (meta match)]
    (reduce (fn [m meta-key]
              (assoc m
                (keyword (name meta-key))
                (get metadata meta-key)))
            {}
            [:parcera.core/start :parcera.core/end])))

(defn- subtree->code-match
  [match options]
  {:match (p/unparse-code match options)
   :meta  (match-meta match)})

;; -------------------
;; Code matching
;; -------------------

;; NOTE we lose leading whitespace, e.g.:
;; Code:
;;   "(let [a 42]
;;   |  (+ a
;;   |     a))"
;; Pattern: "(+ $ $)"
;; Result:
;;   "(+ a
;;   |     a)"
;; Instead of:
;;   "  (+ a
;;   |     a)"
;; This can be fixed by the caller using :start :column in :meta.
(defn find-codes
  "Find pieces of `code` based on `pattern`. Return a lazy sequence of matchs
   where each one is a map of :match and :meta, respectively the matching code
   and its location metadata.

   `pattern` must have been constructed using grape.core/pattern.

   `options` are passed downstream. The only supported one for now is :inline?,
   which forces matches on a single line."
  ([code pattern]
   (find-codes code pattern nil))
  ([code pattern options]
   {:pre [(pattern? pattern)]}
   (->> (find-subtrees
          (parse-code code)
          pattern)
        (map #(subtree->code-match % options)))))

(defn count-codes
  "Equivalent to (count (find-codes code pattern))."
  [code pattern]
  {:pre [(pattern? pattern)]}
  (count-subtrees
    (parse-code code)
    pattern))

(defn find-code
  "Equivalent of (first (find-codes code pattern))."
  [code pattern]
  (first (find-codes code pattern)))
