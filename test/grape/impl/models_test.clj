(ns grape.impl.models-test
  (:require [clojure.test :refer :all]
            [grape.impl.models :as m]
            [grape.impl.parsing :as p]))

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

(deftest wildcard?-test
  (are [expected node]
    (= expected (boolean (m/wildcard? node)))
    false (list :keyword "$foo")
    false (list :symbol "foo")
    true (list :symbol "$foo")
    true (list :symbol "$list&")))

(deftest multi-wildcard?-test
  (are [expected node]
    (= expected (boolean (m/multi-wildcard? node)))
    false (list :keyword "$foo")
    false (list :symbol "$foo")
    true (list :symbol "$foo&")))

(deftest node-wildcard-test
  (are [expected s]
    (= expected (m/node-wildcard (list :symbol s)))
    nil "$nope"
    nil "$nope&"
    {:node-type :_all :multiple? false} "$"
    {:node-type :keyword :multiple? false} "$keyword"
    {:node-type :macro_keyword :multiple? false} "$macro-keyword"

    {:node-type :_all :multiple? true} "$&"
    {:node-type :keyword :multiple? true} "$keyword&"))


(deftest compact-whitespaces-test
  (testing "leading/trailing whitespaces"
    (let [compact '(:code (:vector (:symbol "a") (:whitespace " ") (:symbol "b")))]
      (are [original]
        (= compact (m/compact-whitespaces original))
        compact
        '(:code (:vector (:symbol "a") (:whitespace " ") (:symbol "b") (:whitespace " ")))
        '(:code (:vector (:whitespace " ") (:symbol "a") (:whitespace " ") (:symbol "b")))
        '(:code (:vector (:whitespace " ") (:symbol "a") (:whitespace " ") (:symbol "b") (:whitespace "  ")))
        '(:code (:whitespace "  ") (:vector (:whitespace " ") (:symbol "a") (:whitespace " ") (:symbol "b"))))))

  (testing "whitespaces in strings"
    (let [code '(:code (:string " a \n "))]
      (is (= code (m/compact-whitespaces code)))))

  (testing "parse/unparse tests"
    (are [expected code]
      (= expected (p/unparse-code (m/compact-whitespaces (p/parse-code code))))
      "foo" " foo "
      "hey" "hey\n"
      "various" "  various\r\t \n"
      "[a b c]" "[a b c] "
      "#_foo bar" "#_ foo\nbar"
      "[a b]" "[a                 b ]"
      "a b c" "a b c"
      "[a b]" "[a,     ,,b]")))