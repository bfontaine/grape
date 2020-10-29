(ns grape.impl.match
  "Internal match utilities."
  (:require [clojure.string :as str]
            [grape.impl.models :as m]
            [grape.impl.parsing :as p]))

(defn pattern
  [code]
  {:post [(m/pattern? %)]}
  (-> code
      str/trim
      p/parse-code
      m/node-children
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

(defn- match-multi-wildcards?
  [trees wildcard-nodes]
  (let [node-types (map :node-type (distinct (map m/node-wildcard wildcard-nodes)))]
    (or
      ;; Empty trees always match any number of multiple-expressions wildcards
      (empty? trees)

      ;; $& matches everything
      (= [:_all] node-types)

      ;; $<type>& matches a sequence of nodes of type <type>
      (and
        (= 1 (count node-types))
        (let [node-type (first node-types)]
          (every? #(= node-type (m/node-type %)) trees)))

      ;; We could add some heuristics at this point.
      ;; - [$t1& $t2& $t3&]: drop trees while type==t1, then while type==t2, etc.
      ;; - [$t1& $& $t2&]: idem, but start from the end once we reach $&
      )))

(defn- match-seq?
  "Test if a sequence of subtrees match a sequence of patterns."
  [trees patterns]
  (let [;; Assume a pattern sequence have the following format:
        ;;   (expression* expressions-wildcard expression*)
        ;; For convenience, we allow multiple equal expressions-wildcards to occur
        ;; as if they were all one wildcard.
        ;;
        ;; We first split the pattern to extract that (expression* part from
        ;; the rest.
        [start end-with-wildcard] (split-with (complement m/multi-wildcard?)
                                              patterns)
        ;; Then split the rest into wildcards and the end.
        [wildcard-nodes end] (split-with m/multi-wildcard? end-with-wildcard)]

    ;; If we have no wildcard, fallback on the default matching.
    (if (empty? wildcard-nodes)
      (exact-match-seq? trees patterns)
      ;; Otherwise, extract the start and end of the subtrees.
      (let [
            ;; Take the right number of subtrees at the beginning so they match 'start' patterns.
            [start-trees rest-trees] (split-at (count start) trees)
            ;; Do the same with the end subtrees.
            [wildcarded-trees end-trees] (split-at (- (count rest-trees) (count end)) rest-trees)]
        (and
          (exact-match-seq? start-trees start)
          (exact-match-seq? end-trees end)
          (match-multi-wildcards? wildcarded-trees wildcard-nodes))))))

(defn- match-wildcard?
  [node pattern]
  (let [{:keys [node-type]} (m/node-wildcard pattern)]
    (or (= :_all node-type)
        (= node-type (m/node-type node)))))

(defn match?
  "Test if a subtree matches a pattern. Always return false on the root tree."
  [tree pattern]
  (cond
    ;; root tree
    (= :code (m/node-type tree))
    false

    ;; one of them is a leave:
    ;; - if both are leaves and equal, return true
    ;; - otherwise false (a leave and a non-leave are never equal)
    (or (m/tree-leave? tree) (m/tree-leave? pattern))
    (= tree pattern)

    (m/wildcard? pattern)
    (match-wildcard? tree pattern)

    ;; [:symbol "foo"] ≠ [:keyword "foo"]
    (not= (m/node-type tree) (m/node-type pattern))
    false

    :else
    (let [tree-children    (m/node-children tree)
          pattern-children (m/node-children pattern)]
      (match-seq? tree-children pattern-children))))