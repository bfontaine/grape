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

(deftest unindent-test
  (testing "Nothing to unindent"
    (are [s]
      (= s (cli/unindent s))
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
      (= un-indented (cli/unindent indented))
      "foo" "    foo"
      "(println foo)" " (println foo)"))

  (testing "multiple lines"
    (are [un-indented indented]
      (= un-indented (cli/unindent indented))
      "(do\n  (println foo))" "  (do\n    (println foo))"
      "  (do\n(println foo))" "    (do\n  (println foo))"
      "    (-> a\n   b\n  c\n d\ne)" "     (-> a\n    b\n   c\n  d\n e)"
      "(defn\n\nf\n\n[]\n\n)" " (defn\n\n f\n\n []\n\n )"))

  (testing "tabs"
    (is (= "(do\n(foo))" (cli/unindent "\t(do\n\t(foo))")))
    (is (= "(do\n\t(foo))" (cli/unindent "\t(do\n\t\t(foo))"))))

  (testing "mix of tabs and spaces"
    (is (= "  (do\n\t(foo))" (cli/unindent "  (do\n\t(foo))")))
    (is (= "  \t(do\n\t  (foo))" (cli/unindent "  \t(do\n\t  (foo))")))
    (is (= "(do\n\t(foo))" (cli/unindent "  \t(do\n  \t\t(foo))")))))

(deftest match-source!-test
  (let [printlns "  (println \"a\")\n  (println \"b\")\n  (println \"c\")"
        source   {:code (str "(do\n" printlns ")")
                  :path "src/abc.clj"}]
    (testing "no filename"
      (is (= (str printlns "\n")
             (with-out-str
               (cli/match-source! source (g/pattern "(println $)") {})))))
    (testing "filename"
      (is (= (str "src/abc.clj:\n" printlns "\n")
             (with-out-str
               (cli/match-source! source (g/pattern "(println $)") {:show-filename? true})))))

    (testing "unindent"
      (is (= "(println \"a\")\n(println \"b\")\n(println \"c\")\n"
             (with-out-str
               (cli/match-source! source (g/pattern "(println $)") {:unindent? true})))))))