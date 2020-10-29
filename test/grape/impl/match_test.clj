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

(deftest match-multi-wildcards?-test
  (testing "matches"
    (are [trees nodes]
      (#'match/match-multi-wildcards? trees nodes)
      [] [(list :symbol "$keyword&")]
      [] [(list :symbol "$&")]
      [(list :keyword "foo") (list :map)] [(list :symbol "$&")]

      [(list :keyword "a") (list :keyword "b") (list :keyword "c")] [(list :symbol "$keyword&")]))

  (testing "mismatches"
    (are [trees nodes]
      (not (#'match/match-multi-wildcards? trees nodes))
      [(list :keyword "foo") (list :map)] [(list :symbol "$map&") (list :symbol "$&")]
      [(list :keyword "a") (list :keyword "b") (list :keyword "c")] [(list :symbol "$number&")])))

(deftest match-seq?-test
  (let [$   (list :symbol "$")
        $&  (list :symbol "$&")
        kw  (list :keyword "foo")
        sym (list :symbol "bar")]
    (testing "matches"
      (are [trees patterns]
        (#'match/match-seq? trees patterns)
        [] []
        [] [$&]
        [] [$& $&]
        [kw sym] [$&]
        [kw sym] [$& $& $&]
        [kw sym] [kw sym]
        [kw sym] [kw $&]
        [kw sym] [$& sym]
        [kw sym] [$ (list :symbol "$symbol&")]
        [kw kw kw kw] [(list :symbol "$keyword&")]
        ))

    (testing "mismatches"
      (are [trees patterns]
        (not (#'match/match-seq? trees patterns))
        [kw kw kw kw sym] [(list :symbol "$keyword&")]
        ))))

(deftest match?-test
  (testing "equal to self"
    (are [x] (#'match/match? x x)
             [:symbol "foo"]
             [:keyword "foo"]
             [:list [:symbol "a"] [:symbol "b"] [:number "42"]]
             [:vector [:discard [:whitespace " "] [:map]] [:keyword "foo"]]))

  (let [w [:whitespace " "]]
    (is (some? (#'match/match?
                 [:list [:symbol "f"] w [:number "1"] w [:number "2"] w [:number "3"]]
                 [:list [:symbol "f"] w [:symbol "$&"] w [:number "2"] w [:number "3"]])))))