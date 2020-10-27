(ns grape.impl.parsing-test
  (:require [clojure.test :refer :all]
            [grape.impl.parsing :as p]))

(deftest parse-code-test
  (is (= (list :code (list :symbol "foo"))
         (p/parse-code "foo"))))

(deftest unparse-code-test
  (testing "round-trips"
    (are [code]
      (= code (p/unparse-code (p/parse-code code)))
      "foo"
      ";; some code\nfoo"))

  (testing "inline"
    ;; See grape.impl.models-test/compact-whitespaces-test for more advanced tests.
    (are [expected code]
      (= expected (p/unparse-code (p/parse-code code) {:inline? true}))
      "foo" "foo"
      "foo" ";; some code\nfoo"
      "(map inc xs)" "(map\n  inc\n  xs)\n")))