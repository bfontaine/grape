(ns grape.cli-test
  (:require [clojure.test :refer :all]
            [grape.cli :as cli]
            [clojure.string :as str]
            [grape.core :as g])
  (:import (java.io File)))

(deftest usage-test
  (is (string? (#'cli/usage ""))))

(deftest version-test
  (let [version (#'cli/version)]
    (is (string? version))
    (is (re-matches #"\d+.\d+\.\d+.*" version))))

(deftest list-clojure-files-test
  (testing "matches"
    (testing "clj directory"
      (is (seq (cli/list-clojure-files ".")))
      (is (seq (cli/list-clojure-files "src"))))
    (testing "missing file/directory"
      (is (empty? (cli/list-clojure-files "/i-dont-exist"))))
    (testing "clj file"
      (is (seq (cli/list-clojure-files "project.clj"))))
    (testing "non-clj file"
      (is (empty? (cli/list-clojure-files "README.md")))))
  (testing "paths"
    (testing "absolute"
      (let [pwd (-> (File. ".") .getAbsolutePath)]
        (are [path]
          (every? #(str/starts-with? % "/") (cli/list-clojure-files path))
          pwd (str pwd "/.") (str pwd "/src") (str "/./" pwd))))
    (testing "relative"
      (are [path]
        (every? #(str/starts-with? % "src/") (cli/list-clojure-files path))
        "src" "./src" "src/." "./src/./"))))

(deftest parse-args-test
  ;; Just check the function works; don't test all the logic done in parse-opts.
  (is (= 0 (:exit-code (cli/parse-args ["grape" "--help"])))))

;; TODO test that we preserve commas, #_ and comments when printing matches
;; Also: preserve newlines: \r\n

(deftest unindent-lines-test
  (letfn [(unindent [s] (cli/join-lines (cli/unindent-lines (cli/split-lines s))))]
    (testing "Nothing to unindent"
      (are [s]
        (= s (unindent s))
        ""
        "\n"
        "\n\n\n"
        "foo"
        "foo\n"
        "\nfoo"
        "(println foo)"
        "(do\n  (println foo))\n"
        "  (do\n(println foo))\n"))

    (testing "single line"
      (are [un-indented indented]
        (= un-indented (unindent indented))
        "foo" "    foo"
        "(println foo)" " (println foo)"))

    (testing "multiple lines"
      (are [un-indented indented]
        (= un-indented (unindent indented))
        "(do\n  (println foo))" "  (do\n    (println foo))"
        "  (do\n(println foo))" "    (do\n  (println foo))"
        "    (-> a\n   b\n  c\n d\ne)" "     (-> a\n    b\n   c\n  d\n e)"
        "(defn\n\nf\n\n[]\n\n)" " (defn\n\n f\n\n []\n\n )"))

    (testing "tabs"
      (is (= "(do\n(foo))" (unindent "\t(do\n\t(foo))")))
      (is (= "(do\n\t(foo))" (unindent "\t(do\n\t\t(foo))"))))

    (testing "mix of tabs and spaces"
      (is (= "  (do\n\t(foo))" (unindent "  (do\n\t(foo))")))
      (is (= "  \t(do\n\t  (foo))" (unindent "  \t(do\n\t  (foo))")))
      (is (= "(do\n\t(foo))" (unindent "  \t(do\n  \t\t(foo))"))))))

(deftest prepend-line-numbers-test
  (testing "one line"
    (are [expected start line]
      (= [expected] (cli/prepend-line-numbers start nil [line]))
      "1:foo" 1 "foo"
      "10000:42" 10000 42))

  (testing "multiple lines"
    (testing "same length"
      (are [expected start lines]
        (= expected (cli/prepend-line-numbers start nil lines))
        ["1:a" "2:b" "3:c"] 1 ["a" "b" "c"]
        ["200:a" "201:b" "202:c"] 200 ["a" "b" "c"])

      (testing "empty lines"
        (is (= ["1:a" "2:" "3:c"] (cli/prepend-line-numbers 1 nil ["a" "" "c"])))))

    (testing "different lengths"
      (are [expected start lines]
        (= expected (cli/prepend-line-numbers start nil lines))
        [" 9:a" "10:b" "11:c"] 9 ["a" "b" "c"]
        [" 8:a" " 9:b" "10:c"] 8 ["a" "b" "c"]
        [" 997:a" " 998:b" " 999:c" "1000:d"] 997 ["a" "b" "c" "d"])
      (let [prefixed (cli/prepend-line-numbers 1 nil (map str (range 1 1001)))]
        (is (= "   1:1" (first prefixed)))
        (is (= "  42:42" (nth prefixed 41)))
        (is (= " 100:100" (nth prefixed 99)))
        (is (= "1000:1000" (last prefixed)))))

    (testing "first-line-number"
      (are [expected start lines]
        (= expected (cli/prepend-line-numbers start {:first-line-number? true} lines))
        ["9:a" "  b" "  c"] 9 ["a" "b" "c"]
        ["1000:a" "     b" "     c"] 1000 ["a" "b" "c"]))))

(deftest split-lines-test
  (are [expected s]
    (= expected (cli/split-lines s))
    ["foo"] "foo"
    ["" "foo"] "\nfoo"
    ["foo" ""] "foo\n"
    ["a" "" "b"] "a\n\nb"
    ["a\r" "b"] "a\r\nb"))

(deftest read-path-test
  (testing "stdin"
    (is (= {:path "(stdin)"
            :code "foo"}
           (with-in-str
             "foo"
             (#'cli/read-path "-"))))

    (testing "multiple times"
      (is (= [{:path "(stdin)"
               :code "foo"}
              {:path "(stdin)"
               :code ""}])
          (with-in-str
            "foo"
            (vector
              (#'cli/read-path "-")
              (#'cli/read-path "-"))))))

  (let [path   "test/grape/cli_test.clj"
        source (#'cli/read-path path)]
    (is (= path (:path source)))
    (is (string? (:code source)))))

(deftest match-source!-test
  (let [printlns "  (println \"a\")\n  (println \"b\")\n  (println \"c\")"
        source   {:code (str "(do\n" printlns ")")
                  :path "src/abc.clj"}
        pattern  (g/pattern "(println $)")]
    (testing "no filename"
      (is (= (str printlns "\n")
             (with-out-str
               (cli/match-source! source pattern {})))))
    (testing "filename"
      (is (= (str "src/abc.clj:\n" printlns "\n")
             (with-out-str
               (cli/match-source! source pattern {:show-filename? true})))))
    (testing "unindent"
      (is (= "(println \"a\")\n(println \"b\")\n(println \"c\")\n"
             (with-out-str
               (cli/match-source! source pattern {:unindent? true})))))
    (testing "line numbers"
      (is (= "2:  (println \"a\")\n3:  (println \"b\")\n4:  (println \"c\")\n"
             (with-out-str
               (cli/match-source! source pattern {:line-numbers? true})))))
    (testing "line numbers + unindent"
      (is (= "2:(println \"a\")\n3:(println \"b\")\n4:(println \"c\")\n"
             (with-out-str
               (cli/match-source! source pattern {:line-numbers? true
                                                  :unindent?     true})))))
    (testing "trailing newlines"
      (is (= "  (println \"a\")\n\n  (println \"b\")\n\n  (println \"c\")\n\n"
             (with-out-str
               (cli/match-source! source pattern {:trailing-newline? true})))))
    (testing "line numbers + trailing newlines"
      (is (= "2:  (println \"a\")\n\n3:  (println \"b\")\n\n4:  (println \"c\")\n\n"
             (with-out-str
               (cli/match-source! source pattern {:line-numbers?     true
                                                  :trailing-newline? true})))))

    (testing "first line number"
      (testing "with single-line matches"
        (let [expected "2:  (println \"a\")\n3:  (println \"b\")\n4:  (println \"c\")\n"]
          (testing "without :line-numbers?"
            (is (= expected
                   (with-out-str
                     (cli/match-source! source pattern {:first-line-number? true})))))

          (testing "with :line-numbers?"
            (is (= expected
                   (with-out-str
                     (cli/match-source! source pattern {:first-line-number? true
                                                        :line-numbers?      true})))))))

      (testing "with multi-line matches"
        (is (= "1:(do\n    (println \"a\")\n    (println \"b\")\n    (println \"c\"))\n"
               (with-out-str
                 (cli/match-source! source (g/pattern "(do $&)") {:first-line-number? true}))))
        (testing "with unindent"
          (is (= "1:(map\n  f\n  xs)\n"
                 (with-out-str
                   (cli/match-source!
                     (assoc source :code "  (map\n  f\n  xs)\n")
                     (g/pattern "(map $&)")
                     {:first-line-number? true
                      :unindent?          true})))))))
    ))