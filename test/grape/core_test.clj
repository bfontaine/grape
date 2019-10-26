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

(deftest find-subtree-exact-match
  (testing "same tree"
    (let [t (g/parse-code simple-defn)
          p (g/parse-pattern simple-defn)]
      (is (= t (g/find-subtree t p)))))

  (let [t (g/parse-code simple-ns)]
    (testing "top-level tree"
      (is (= [:code (last t)]
             (g/find-subtree t (g/parse-pattern "(defn g [x] x)")))))

    (testing "deep match"
      (are [code] (= (g/parse-code code)
                     (g/find-subtree t
                                     (g/parse-pattern code)))
        "defn" "f" "[x]"
        "(* 2 x)" "2" "x" "*"
        "my.ns" "[clojure.string :as str]"))))

(deftest find-exact-subtree-no-match
  (testing "root atoms"
    (let [codes ["a" "b" "x" "1" "2" "-1" ":foo" ":a/foo" "true" "false" "2.3"
                 "\"hey I'm a string\"" "\\space"]]
      (doseq [code1 codes
              code2 codes]
        (when (not= code1 code2)
          (is (nil? (g/find-subtree
                      (g/parse-code code1)
                      (g/parse-pattern code2))))))))

  (testing "length mismatch"
    (let [code (g/parse-code "[1 2 3]")]
      (are [pattern] (nil? (g/find-subtree code (g/parse-pattern pattern)))
           "[]"
           "[1]"
           "[1 2]"
           "[123]"
           "[12 3]"
           "[1 2 3 4]"
           "[1 2 3 4 5 6 7]"))))

;; -------------------
;; Code matching
;; -------------------

(deftest find-codes-exact-match
  (let [code "(def a 1)"]
    (is (= [{:match code
             :meta {:start-column 1, :end-column 10
                    :start-line 1, :end-line 1
                    :start-index 0, :end-index 9}}]
           (g/find-codes code code)))))
