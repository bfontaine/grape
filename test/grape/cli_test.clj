(ns grape.cli-test
  (:require [clojure.test :refer :all]
            [grape.cli :as cli]
            [clojure.string :as str])
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