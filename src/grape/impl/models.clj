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

;; -------------------
;; Transformations
;; -------------------

(defn- remove-whitespaces
  "Drop whitespaces, comments and discarded forms from a sequence of nodes."
  [xs]
  (remove #(#{:whitespace :comment :discard} (node-type %)) xs))

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

(defn compact-whitespaces
  "Transform a tree by 'compacting' its whitespaces: all newlines and sequences of whitespaces are replaced
   by a single whitespace. Comments are removed as well."
  [tree]
  (->> tree
       (postwalk
         (fn [node]
           (if (tree-node? node)
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
                    remove-trailing-whitespace))
             node)))
       remove-leading-whitespace
       remove-trailing-whitespace))

(defn node-children
  "Return non-whitespace node children."
  [node]
  (remove-whitespaces (rest node)))

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

(defn wildcard-expression?
  "Test if a node is an expression wildcard symbol."
  [node]
  (= (list :symbol *wildcard-expression*)
     node))

(defn wildcard-expressions?
  "Test if a node is an expressions wildcard symbol."
  [node]
  (= (list :symbol *wildcard-expressions*)
     node))

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

(defn typed-wildcard-expression?
  "Test if a node is an typed expression wildcard symbol."
  [node]
  (and (pattern? node)
       (= :symbol (node-type node))
       (let [s (node-child node)]
         (str/starts-with? s *wildcard-expression*)
         (contains? types (subs s 1)))))

(defn ->typed-wildcard
  [typ]
  (str *wildcard-expression* typ))