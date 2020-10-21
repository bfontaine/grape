(ns grape.impl.parsing-test
  (:require [clojure.test :refer :all]
            [grape.impl.parsing :as p]))

(deftest parse-code-test
  (is (= (list :code (list :symbol "foo"))
         (p/parse-code "foo"))))

(deftest unparse-code-test
  (is (= "foo"
         (p/unparse-code (p/parse-code "foo")))))