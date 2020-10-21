(ns grape.impl.models
  "Internal models."
  (:require [clojure.string :as str]))

(defn tree-node?
  [x]
  (and (sequential? x)
       (keyword? (first x))))

(def tree-leave? string?)

(def pattern? tree-node?)

(def node-type first)
(def node-children rest)
(def node-child second)

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

(defn typed-wildcard-expression?
  "Test if a node is an typed expression wildcard symbol."
  [node]
  (and (pattern? node)
       (= :symbol (node-type node))
       (not (wildcard-expression? node))
       (not (wildcard-expressions? node))
       (str/starts-with? (node-child node) *wildcard-expression*)))

(defn ->typed-wildcard
  [typ]
  (str *wildcard-expression* typ))