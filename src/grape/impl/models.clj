(ns grape.impl.models
  "Internal models."
  (:require [clojure.string :as str]
            [clojure.walk :refer [postwalk]]))

(defn tree-node?
  [x]
  (and (sequential? x)
       (keyword? (first x))))

(def tree-leave? string?)

(def pattern? tree-node?)

(def node-type first)
(def node-child second)
(def ^:private raw-node-children rest)

(defn- remove-whitespaces
  "Drop whitespaces, comments and discarded forms from a sequence of nodes."
  [xs]
  (remove #(#{:whitespace :comment :discard} (node-type %)) xs))

(defn node-children
  "Return non-whitespace node children."
  [node]
  (remove-whitespaces (rest node)))

;; -------------------
;; Wildcards
;; -------------------

(def ^:private
  types
  ;; https://github.com/carocad/parcera/blob/d6b28b1058ef2af447a9452f96c7b6053e59f613/src/parcera/core.cljc#L26
  #{"backtick"
    "character"
    "conditional"
    "conditional-splicing"
    "deref"
    "fn"
    "keyword"
    "list"
    "macro-keyword"
    "map"
    "metadata"
    "number"
    "quote"
    "regex"
    "set"
    "string"
    "symbol"
    "symbolic"
    "unquote"
    "unquote-splicing"
    "vector"
    "var-quote"})

(def wildcard-prefix "$")
(def multiple-suffix "&")

(defn wildcard?
  "Test if a node is a wildcard. This doesn’t check if it's a valid one."
  [node]
  (and (pattern? node)
       (= :symbol (node-type node))
       (str/starts-with? (node-child node) wildcard-prefix)))

(defn multi-wildcard?
  "Test if a node is a multiple-expressions wildcard. This doesn’t check if its type (if any) is valid."
  [node]
  (and (wildcard? node)
       (str/ends-with? (node-child node) multiple-suffix)))

(defn node-wildcard
  "Given a node for which `(wildcard? node)` is true, return a map describing the wildcard if it’s valid."
  [node]
  (let [s             (node-child node)
        multiple?     (str/ends-with? s multiple-suffix)
        wildcard-type (subs s 1 (cond-> (count s) multiple? dec))]
    (cond
      (= "" wildcard-type)
      {:node-type :_all
       :multiple? multiple?}

      (contains? types wildcard-type)
      {:node-type (keyword (str/replace wildcard-type #"-" "_"))
       :multiple? multiple?})))

;; -------------------
;; Transformations
;; -------------------

(defn- remove-whitespace*
  "Remove a specific node from a tree if it's a whitespace. get-fn takes the tree's children and return the node.
   remove-fn takes the children and return them without that node."
  [tree get-fn remove-fn]
  (let [children (raw-node-children tree)
        node     (get-fn children)]
    (if (= :whitespace (node-type node))
      (cons (first tree) (remove-fn children))
      tree)))

(defn- remove-trailing-whitespace
  "Remove the trailing whitespace node of a tree, if any."
  [tree]
  (remove-whitespace* tree last butlast))

(defn- remove-leading-whitespace
  "Remove the leading whitespace node of a tree, if any."
  [tree]
  (remove-whitespace* tree first rest))

(defn- postwalk-tree
  "Equivalent of postwalk that only calls f on tree nodes."
  [f tree]
  (postwalk
    (fn [node]
      (if (tree-node? node)
        (f node)
        node))
    tree))

(defn compact-whitespaces
  "Transform a tree by 'compacting' its whitespaces: all newlines and sequences of whitespaces are replaced
   by a single whitespace. Comments are removed as well."
  [tree]
  (->> tree
       (postwalk-tree
         (fn [node]
           (case (node-type node)
             :comment nil
             :whitespace (let [s (node-child node)]
                           (if (or (= \newline (first s))
                                   (< 1 (count s)))
                             '(:whitespace " ")
                             node))
             (->> node
                  (remove nil?)
                  remove-leading-whitespace
                  remove-trailing-whitespace))))
       remove-leading-whitespace
       remove-trailing-whitespace))