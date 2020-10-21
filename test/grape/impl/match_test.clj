(ns grape.impl.match-test
  (:require [clojure.test :refer :all]
            [grape.impl.match :as match]))

(deftest match?-test
  (testing "equal to self"
    (are [x] (#'match/match? x x)
             [:whitespace " "]
             [:symbol "foo"]
             [:keyword "foo"]
             [:list [:symbol "a"] [:symbol "b"] [:number "42"]]
             [:discard [:whitespace " "]])))
