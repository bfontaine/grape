(ns grape.impl.match
  (:require [clojure.string :as str]
            [grape.impl.models :refer [tree-leave? pattern? node-type node-children node-child
                                       wildcard-expression? wildcard-expressions? typed-wildcard-expression?
                                       ->typed-wildcard]]
            [grape.impl.parsing :as p]))

(defn pattern
  [code]
  {:post [(pattern? %)]}
  (-> code
      str/trim
      p/parse-code
      node-children
      first))

;; -------------------
;; Matching trees
;; -------------------

(declare match?)

(defn- exact-match-seq?
  "Test if a sequence of subtrees match a sequence of patterns. This ignores any wildcard."
  [trees patterns]
  (and
    (= (count trees) (count patterns))
    (every? true?
            (map match?
                 trees
                 patterns))))

(defn- match-seq?
  "Test if a sequence of subtrees match a sequence of patterns."
  [trees patterns]
  (let [;; Assume a pattern sequence have the following format:
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
        node-type-as-a-wildcard-name (->typed-wildcard node-type)]
    (= wildcard-name
       node-type-as-a-wildcard-name)))

(defn match?
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