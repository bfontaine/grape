(ns grape.cli-test
  (:require [clojure.test :refer :all]
            [grape.cli :as cli]))

(deftest version-test
  (let [version (#'cli/version)]
    (is (string? version))
    (is (re-matches #"\d+.\d+\.\d+.*", version))))

(deftest list-clojure-files-test
  (testing "clj directory"
    (is (seq (cli/list-clojure-files ".")))
    (is (seq (cli/list-clojure-files "src"))))
  (testing "missing file/directory"
    (is (empty? (cli/list-clojure-files "/i-dont-exist"))))
  (testing "clj file"
    (is (seq (cli/list-clojure-files "project.clj"))))
  (testing "non-clj file"
    (is (empty? (cli/list-clojure-files "README.md")))))