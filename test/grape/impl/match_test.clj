(ns grape.impl.match-test
  (:require [clojure.test :refer :all]
            [grape.impl.match :as match]))

(deftest parse-pattern-test
  (testing "leading whitespace and comments"
    (are [pattern] (= '(:number "42") (match/pattern pattern))
                   "42"
                   "    42"
                   "\n42"
                   ";; this is my pattern\n42"))

  (testing "discard"
    (are [pattern] (= '(:number "42") (match/pattern pattern))
                   "#_ 41 42"
                   "#_ #_ 1 2 42"
                   "#_ 1 #_ 2 42 #_ 3"
                   "#_ (1 2 3) 42"
                   "#_ ;; discard the next element\n1 42")))

(deftest match?-test
  (testing "equal to self"
    (are [x] (#'match/match? x x)
             [:whitespace " "]
             [:symbol "foo"]
             [:keyword "foo"]
             [:list [:symbol "a"] [:symbol "b"] [:number "42"]]
             [:discard [:whitespace " "]])))
