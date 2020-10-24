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
               (cli/match-source! source (g/pattern "(println $)") {:show-filename? true})))))))