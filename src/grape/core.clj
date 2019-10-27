(ns grape.core
  (:require [clojure.string :as str]
            [instaparse.core :as insta]
            [parcera.core :as parcera]))

(def ^:private tree-node? vector?)
(def ^:private tree-leave? string?)

(def ^:private node-type first)
(def ^:private node-children rest)

;; -------------------
;; Parsing code & patterns
;; -------------------

(defn- parse-string
  "Parse a string as an AST."
  [code]
  (parcera/clojure code))

(defn parse-code
  "Parse code."
  [code]
  ;; https://github.com/Engelberg/instaparse#line-and-column-information
  (insta/add-line-and-column-info-to-metadata
    code
    (parse-string code)))

(defn- drop-whitespace
  "Drop whitespaces and discarded forms from a sequence of nodes. Not lazy."
  [xs]
  (:nodes
    (reduce (fn [{:keys [discard nodes] :as acc} node]
              ;; Parcera parses discard macros as standalone elements. We have
              ;; to "apply" them ourselves.
              (cond
                ;; drop whitespace
                (= :whitespace (node-type node))
                acc

                ;; discard macro
                (= :discard (node-type node))
                (update acc :discard inc)

                ;; discarded
                (pos-int? discard)
                (update acc :discard dec)

                :else
                (update acc :nodes #(conj % node))))
            {:nodes   []
             :discard 0}
            xs)))

(defn pattern
  "Parse a piece of code as a pattern to be matched. Any expression after the
   first one is discared as well as comments and discarded forms."
  [code]
  (-> code
      str/trim
      parse-string
      node-children
      drop-whitespace
      first))


;; -------------------
;; Wildcards
;; -------------------

(def ^{:dynamic true
       :doc "Wildcard symbol used to represent any single expression in a pattern.
This must be a valid Clojure symbol."}
  *wildcard-expression*
  "$")

(defn- wildcard-expression?
  [node]
  (= [:symbol *wildcard-expression*]
     node))

;; -------------------
;; Matching trees
;; -------------------

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

    ;; [:symbol "foo"] ≠ [:simple-keyword "foo"]
    (not= (node-type tree) (node-type pattern))
    false

    :else
    (let [tree-children    (drop-whitespace (node-children tree))
          pattern-children (drop-whitespace (node-children pattern))]
      (if (not= (count tree-children) (count pattern-children))
        false
        (every? true?
                (map match?
                     tree-children
                     pattern-children))))))

;; -------------------
;; High-level tree functions
;; -------------------

(defn- wrap-code-parent
  "Wrap a tree in a :code parent with the same metadata."
  [tree]
  (with-meta [:code tree] (meta tree)))

(defn find-subtrees
  "Match a tree given a subtree pattern. Return a lazy sequence of subtrees."
  [tree pattern]
  (->> tree
       (tree-seq tree-node? node-children)
       (filter #(match? % pattern))
       (map wrap-code-parent)))

(defn find-subtree
  "Equivalent of (first (find-subtrees tree pattern))."
  [tree pattern]
  ;; first implementation: one match + one expression w/ no wildcard
  (first (find-subtrees tree pattern)))

(def ^:private
  meta-keys
  [:instaparse.gll/start-column, :instaparse.gll/end-column
   :instaparse.gll/start-line, :instaparse.gll/end-line
   :instaparse.gll/start-index, :instaparse.gll/end-index])

(defn- match-meta
  "Extract the Instaparse-added location metadata from a match and return it."
  [match]
  (when-let [metadata (meta match)]
    (reduce (fn [m meta-key]
              (assoc m
                     (keyword (name meta-key))
                     (get metadata meta-key)))
            {}
            meta-keys)))

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
;;
(defn find-codes
  "Find pieces of `code` based on `pattern`. Return a lazy sequence of matchs
   where each one is a map of :match and :meta, respectively the matching code
   and its location metadata.

   `pattern` must have been constructed using grape.core/pattern."
  [code pattern]
  {:pre [(vector? pattern)]}
  (->> (find-subtrees
         (parse-code code)
         pattern)
       (map subtree->code-match)))

(defn find-code
  "Equivalent of (first (find-codes code pattern))."
  [code pattern]
  (first (find-codes code pattern)))
