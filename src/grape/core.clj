(ns grape.core
  (:require [clojure.string :as str]
            [instaparse.core :as insta]
            [parcera.core :as parcera]))

(def ^:private tree-node? vector?)
(def ^:private tree-leave? string?)

(def ^:private node-type first)
(def ^:private node-children rest)

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

(defn parse-pattern
  "Parse a code pattern to be matched against parsed code. Any expression
   after the first one is discared."
  [code]
  (-> code
      str/trim
      parse-string
      node-children
      first))

(defn- drop-whitespace
  [xs]
  (remove #(= :whitespace (node-type %)) xs))

(defn- match?
  "Test if a subtree matches a pattern. Always return false on the root tree."
  [tree pattern]
  (cond
    (= :code (node-type tree))
    false

    ;; one of them is a leave:
    ;; - if both are leaves and equal, return true
    ;; - otherwise false (a leave and a non-leave are never equal)
    (or (tree-leave? tree) (tree-leave? pattern))
    (= tree pattern)

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

(defn- ->code
  "Wrap a tree in a :code parent with the same metadata."
  [tree]
  (with-meta [:code tree] (meta tree)))

(defn find-subtrees
  "Match a tree given a subtree pattern. Return a lazy sequence of subtrees."
  [tree pattern]
  (->> tree
       (tree-seq tree-node? node-children)
       (filter #(match? % pattern))
       (map ->code)))

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

(defn find-codes
  "Find pieces of `code` based on `pattern`. Return a sequence of matchs where
   each one is a map of :match and :meta, respectively the matching code and
   its location metadata."
  [code pattern]
  (->> (find-subtrees
         (parse-code code)
         (parse-pattern pattern))
       (map (fn [match]
              {:match (parcera/code match)
               :meta  (match-meta match)}))))

(defn find-code
  "Equivalent of (first (find-codes code pattern))."
  [code pattern]
  (first (find-codes code pattern)))
