(ns grape.core
  (:require [clojure.string :as str]
            [parcera.core :as parcera]))

(defn- tree-node?
  [x]
  (and (sequential? x)
       (keyword? (first x))))

(def ^:private tree-leave? string?)

(def ^:private pattern?
  tree-node?)

(def ^:private node-type first)
(def ^:private node-children rest)
(def ^:private node-child second)

;; -------------------
;; Parsing code & patterns
;; -------------------

(defn parse-code
  "Parse code."
  [code]
  (parcera/ast code))

(defn- drop-whitespace
  "Drop whitespaces, comments and discarded forms from a sequence of nodes."
  [xs]
  (remove #(#{:whitespace :comment :discard} (node-type %)) xs))

(defn pattern
  "Parse a piece of code as a pattern to be matched. Any expression after the
   first one is discarded as well as comments and discarded forms."
  [code]
  {:post [(pattern? %)]}
  (-> code
      str/trim
      parse-code
      node-children
      drop-whitespace
      first))


;; -------------------
;; Wildcards
;; -------------------

(def ^{:dynamic true
       :doc     "Wildcard symbol used to represent any single expression in a pattern.
This must be a valid Clojure symbol.
It is also used as a prefix for typed wildcards. For example, if this is set to
$ (the default), $string represents any single string expression; $list any
single list expression; etc."}
  *wildcard-expression*
  "$")

(def ^{:dynamic true
       :doc     "Wildcard symbol used to represent any number of expressions
in a pattern, including zero. This must be a valid Clojure symbol."}
  *wildcard-expressions*
  "$&")

(defn- wildcard-expression?
  [node]
  (= (list :symbol *wildcard-expression*)
     node))

(defn- wildcard-expressions?
  [node]
  (= (list :symbol *wildcard-expressions*)
     node))

(defn- typed-wildcard-expression?
  [node]
  (and (pattern? node)
       (= :symbol (node-type node))
       (str/starts-with? (node-child node) *wildcard-expression*)))

;; -------------------
;; Matching trees
;; -------------------

(declare match?)

(defn- exact-match-seq?
  "Test if a sequence of subtrees match a sequence of patterns. This ignores
   any expressions wildcard."
  [trees patterns]
  (if (not= (count trees) (count patterns))
    false
    (every? true?
            (map match?
                 trees
                 patterns))))

(defn- match-seq?
  "Test if a sequence of subtrees match a sequence of patterns."
  [trees patterns]
  (let [trees    (drop-whitespace trees)
        patterns (drop-whitespace patterns)

        ;; Assume a pattern sequence have the following format:
        ;;   (expression* expressions-wildcard expression*)
        ;; For convenience, we allow multiple expressions-wildcards to occur
        ;; as if they were all one wildcard.
        ;;
        ;; We first split the pattern to extract that (expression* part from
        ;; the rest.
        [start end-with-wildcard] (split-with (complement wildcard-expressions?)
                                              patterns)
        ;; Then split the rest into wildcards and the end.
        [wildcards end] (split-with wildcard-expressions? end-with-wildcard)]

    ;; If we have no wildcard, fallback on the default matching.
    (if (empty? wildcards)
      (exact-match-seq? trees patterns)
      ;; Otherwise, extract the start and end of the subtrees.
      (let [
            ;; Take the right number of subtrees at the beginning so they match
            ;; 'start' patterns.
            start-trees (take (count start) trees)
            ;; Do the same with the end subtrees.
            end-trees   (drop (- (count trees) (count end)) trees)]
        (and
          (exact-match-seq? start-trees start)
          (exact-match-seq? end-trees end))))))

(defn- match-typed-wildcard-expression?
  "Test if 'node' matches typed-wildcard 'pattern'."
  [node pattern]
  ;; pattern == (:symbol wildcard-name), with wildcard-name = $something
  ;; node == (:something_else ...)
  ;; we're checking if $something-else == $something
  (let [wildcard-name                (node-child pattern)
        node-type                    (str/replace (name (node-type node)) #"_" "-")
        node-type-as-a-wildcard-name (str *wildcard-expression* node-type)]
    (= wildcard-name
       node-type-as-a-wildcard-name)))

(defn- match?
  "Test if a subtree matches a pattern. Always return false on the root tree."
  [tree pattern]
  (cond
    ;; root tree
    (= :code (node-type tree))
    false

    ;; one of them is a leave:
    ;; - if both are leaves and equal, return true
    ;; - otherwise false (a leave and a non-leave are never equal)
    (or (tree-leave? tree) (tree-leave? pattern))
    (= tree pattern)

    (wildcard-expression? pattern)
    true

    (typed-wildcard-expression? pattern)
    (match-typed-wildcard-expression? tree pattern)

    ;; [:symbol "foo"] ≠ [:keyword "foo"]
    (not= (node-type tree) (node-type pattern))
    false

    :else
    (let [tree-children    (node-children tree)
          pattern-children (node-children pattern)]
      (match-seq? tree-children pattern-children))))

;; -------------------
;; High-level tree functions
;; -------------------

(defn- wrap-code-parent
  "Wrap a tree in a :code parent with the same metadata."
  [tree]
  (with-meta [:code tree] (meta tree)))

(defn- find-raw-subtrees
  [tree pattern]
  (->> tree
       (tree-seq tree-node? node-children)
       (filter #(match? % pattern))))

(defn find-subtrees
  "Match a tree given a subtree pattern. Return a lazy sequence of subtrees."
  [tree pattern]
  (map wrap-code-parent
       (find-raw-subtrees tree pattern)))

(defn count-subtrees                                                  ;; TODO test me
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
  [match]
  {:match (parcera/code match)
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
;; This can be fixed by the caller using :start-colum in :meta.
(defn find-codes
  "Find pieces of `code` based on `pattern`. Return a lazy sequence of matchs
   where each one is a map of :match and :meta, respectively the matching code
   and its location metadata.

   `pattern` must have been constructed using grape.core/pattern."
  [code pattern]
  {:pre [(pattern? pattern)]}
  (->> (find-subtrees
         (parse-code code)
         pattern)
       (map subtree->code-match)))

(defn count-codes                                                     ;; TODO test me
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
