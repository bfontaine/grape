(ns grape.impl.models-test
  (:require [clojure.test :refer :all]
            [grape.impl.models :as m]))

(deftest tree-node?-test
  (are [x] (m/tree-node? x)
           (list :keyword "foo")
           (list :list (list :keyword "x") (list :symbol "y"))
           (list :whitespace " "))
  (are [x] (not (m/tree-node? x))
           "terminal"))

(deftest tree-leave?-test
  (is (m/tree-leave? " "))
  (is (not (m/tree-leave? (list :keyword "abc")))))

(deftest pattern?-test
  (are [x] (m/pattern? x)
           (list :symbol "$")
           (list :keyword "a")
           (list :vector)))

(deftest node-type-test
  (is (= :foo (m/node-type (list :foo "terminal")))))

(deftest node-children-test
  (let [children (map #(list :keyword %) ["a" "b" "c"])]
    (is (= children
           (m/node-children (cons :vector children))))))

(deftest node-child-test
  (let [s (list :symbol "foo")]
    (is (= s (m/node-child (list :vector s))))))

(deftest wildcard-expression?-test
  (is (m/wildcard-expression? (list :symbol m/*wildcard-expression*)))
  (is (not (m/wildcard-expression? (list :symbol m/*wildcard-expressions*))))
  (is (not (m/wildcard-expression? (list :keyword m/*wildcard-expression*))))
  (is (not (m/wildcard-expression? (list :symbol "hello")))))

(deftest wildcard-expressions?-test
  (is (m/wildcard-expressions? (list :symbol m/*wildcard-expressions*)))
  (is (not (m/wildcard-expressions? (list :symbol m/*wildcard-expression*))))
  (is (not (m/wildcard-expressions? (list :keyword m/*wildcard-expressions*))))
  (is (not (m/wildcard-expressions? (list :symbol "hello")))))

(deftest typed-wildcard-expression?-test
  (are [x] (m/typed-wildcard-expression?
             (list :symbol (str m/*wildcard-expression* x)))
           "keyword"
           "string"
           "vector")
  (are [x] (not (m/typed-wildcard-expression? x))
           (list :symbol m/*wildcard-expression*)
           (list :symbol m/*wildcard-expressions*)
           (list :keyword (str m/*wildcard-expression* "string")))

  (binding [m/*wildcard-expression* "*"]
    (is (m/typed-wildcard-expression? (list :symbol "*list")))))

(deftest ->typed-wildcard-test
  (binding [m/*wildcard-expression* "*"]
    (is (= "*list"
           (m/->typed-wildcard "list")))))