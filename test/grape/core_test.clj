(ns grape.core-test
  (:require [clojure.test :refer :all]
            [grape.core :as g]))

(def simple-defn
  "(defn f [x] x)")

(def simple-ns
  "(ns my.ns
    (:require [clojure.string :as str]))

  (defn f
    \"hey I'm a docstring\"
    [x]
    (* 2 x))

  (defn g [x] x)")

;; -------------------
;; Tree matching
;; -------------------

(deftest find-subtrees-test
  (testing "wildcards"
    (let [$ (g/pattern "$")]
      (testing "whitespaces don't match"
        (is (= '((:code (:vector (:whitespace " "))))
               (g/find-subtrees (g/parse-code "[ ]")
                                $)))

        (are [code] (empty? (g/find-subtrees (g/parse-code code)
                                             $))
                    "    "
                    ",,,,"
                    "#_ nope"
                    ";; something"
                    "   \n\t ;; blabla\n; bla\n\n\n  ,")))))

(deftest find-subtree-test
  (testing "exact-match"
    (testing "same tree"
      (are [code] (let [t (g/parse-code code)
                        p (g/pattern code)]
                    (= t (g/find-subtree t p)))

                  simple-defn
                  "(map f a)"
                  "(map f a b)"
                  "(+ 1 2)"))

    (let [t (g/parse-code simple-ns)]
      (testing "top-level tree"
        (is (= [:code (last t)]
               (g/find-subtree t (g/pattern "(defn g [x] x)")))))

      (testing "deep match"
        (are [code] (= (g/parse-code code)
                       (g/find-subtree t
                                       (g/pattern code)))
                    "defn" "f" "[x]"
                    "(* 2 x)" "2" "x" "*"
                    "my.ns" "[clojure.string :as str]"))))

  (testing "no match"
    (testing "root atoms"
      (let [codes ["a" "b" "x" "1" "2" "-1" ":foo" ":a/foo" "true" "false" "2.3"
                   "\"hey I'm a string\"" "\\space"]]
        (doseq [code1 codes
                code2 codes]
          (when (not= code1 code2)
            (is (nil? (g/find-subtree
                        (g/parse-code code1)
                        (g/pattern code2))))))))

    (testing "length mismatch"
      (let [code (g/parse-code "[1 2 3]")]
        (are [pattern] (nil? (g/find-subtree code (g/pattern pattern)))
                       "[]"
                       "[1]"
                       "[1 2]"
                       "[123]"
                       "[12 3]"
                       "[1 2 3 4]"
                       "[1 2 3 4 5 6 7]")))))

(deftest count-subtrees-test
  (let [code (g/parse-code "(let [a {:a 1} b {:b 2}] {:a a, :b b})")]
    (are [expected pattern] (= expected (g/count-subtrees code (g/pattern pattern)))
                            0 "$set"
                            0 "42"
                            1 "1"
                            2 ":a"
                            2 "$number"
                            1 "$vector"
                            1 "(let $ $)"
                            3 "$map"
                            4 "$keyword"
                            5 "$symbol"
                            ; 5 symbols + 4 keywords + 2 numbers + 3 maps + 1 vector + the whole expression
                            16 "$")))

;; -------------------
;; Code matching
;; -------------------

(deftest find-codes-exact-match
  (let [code "(def a 1)"]
    (is (= [{:match code
             :meta  {:start {:row 1 :column 0}
                     :end   {:row 1 :column 9}}}]
           (g/find-codes code (g/pattern code))))))

(deftest find-code-ignoring-whitespace
  (let [pattern "(def f [x y] 42)"]
    (are [code] (= {:match code}
                   (dissoc (g/find-code code (g/pattern pattern)) :meta))

                "( \t\t  def\n \t  f   [  x \n\n y  ] 42   )"
                "(def f [x y] 42\n   )"
                "(def f\n  [x y]\n  42)"
                )))

(deftest find-code-expression-wildcard
  (is (some? (g/find-code "(defn f [] 42)" (g/pattern "$"))))
  (is (some? (g/find-code "(let [a (range 3)]
                             (map * a a))"
                          (g/pattern "(map $ $ $)")))))

(deftest find-code-expressions-wildcard
  (let [code "(f 1 2 3)"]
    (are [pattern] (= {:match code}
                      (dissoc (g/find-code code (g/pattern pattern)) :meta))
                   "(f 1 2 3)"
                   "($&)"
                   "($& $&)"
                   "(f $&)"
                   "(f 1 $&)"
                   "(f 1 2 $&)"
                   "(f 1 2 3 $&)"
                   "($& f 1 2 3)"
                   "($& 1 2 3)"
                   "(f $& 2 3)"
                   "(f $& 3)"
                   "(f 1 $& 2 3)")

    (are [pattern] (nil? (g/find-code code (g/pattern pattern)))
                   "(f $& 1 2 3 $&)"
                   "($& f $& 1 $& 2 3)"
                   "($& 1 $& $& $&)")))

(deftest find-code-typed-expression-wildcard
  (let [code "(defn f \"hey\" [x] (+ x 2))"]
    (are [pattern] (= {:match code}
                      (dissoc (g/find-code code (g/pattern pattern)) :meta))
                   "(defn f $string [x] (+ x 2))"
                   "($symbol $symbol $string $vector $list)"
                   "(defn $symbol \"hey\" [x] ($symbol x 2))"
                   )))

(deftest find-code-typed-expression-wildcard-mismatch
  (let [code "(defn f \"hey\" [x] (+ x 2))"]
    (are [pattern] (nil? (g/find-code code (g/pattern pattern)))
                   "(defn f $string $list (+ x 2))"
                   "($symbol $string $symbol $vector $list)"
                   "(defn $symbol \"hy\" [x] ($symbol x 2))")))

(deftest find-code-typed-expression-all-wildcards
  (are [pattern code] (= {:match code}
                         (dissoc (g/find-code code (g/pattern pattern)) :meta))
                      ;; https://github.com/carocad/parcera/blob/d6b28b1058ef2af447a9452f96c7b6053e59f613/src/parcera/core.cljc#L26
                      "$symbol" "foo"
                      "$symbol" "foo/bar"
                      "$symbol" "clojure.string/foo"
                      "$string" "\"foo\""
                      "$keyword" ":foo"
                      "$macro-keyword" "::foo"
                      "$regex" "#\"foo\""
                      "$symbolic" "##Inf"
                      "$number" "42"
                      "$number" "3.14"
                      "$character" "\\a"
                      "$character" "\\space"

                      "$deref" "@foo"
                      "$quote" "'foo"
                      "$unquote" "~foo"
                      "$unquote-splicing" "~@foo"
                      "$backtick" "`foo"
                      "$var-quote" "#'foo"

                      "$conditional" "#?(:cljs nil)"
                      "$conditional-splicing" "#?@(:cljs nil)"

                      "$metadata" "^:foo yo"

                      "$fn" "#(do %)"

                      "$list" "(f 2 3)"
                      "$vector" "[]"

                      "$map" "{}"
                      "$set" "#{}"
                      ))

(deftest find-code-mixed-wildcards
  (let [pattern (g/pattern "#{$ $&}")]
    (is (nil? (g/find-code "#{}" pattern)))
    (is (some? (g/find-code "#{1}" pattern)))
    (is (some? (g/find-code "#{1 2 3}" pattern))))

  (let [pattern (g/pattern "#{0 $ 2 $&}")]
    (is (nil? (g/find-code "#{0 2 3}" pattern)))
    (is (nil? (g/find-code "#{0 1 3}" pattern)))
    (is (some? (g/find-code "#{0 1 2}" pattern)))))

(deftest count-codes-test
  (is (= 5 (g/count-codes "[1 2 3 4]" (g/pattern "$")))))
