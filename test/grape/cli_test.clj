(ns grape.cli-test
  (:require [clojure.test :refer :all]
            [grape.cli :as cli]))

(deftest version-test
  (let [version (#'cli/version)]
    (is (string? version))
    (is (re-matches #"\d+.\d+\.\d+.*", version))))