(ns grape.cli
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [clojure.tools.cli :as cli]
            [grape.core :as g])
  (:gen-class))

(def cli-options
  [["-h" "--help"]])

(defn- usage
  [options-summary]
  (str/join
    \newline
    ["Usage: grape [options] PATTERN [FILE [FILE ...]]"
     ""
     "Options:"
     options-summary
     ""]))

(defn- exit
  [code msg]
  (binding [*out* *err*]
    (println msg))
  (System/exit code))

(defn parse-args
  [args]
  (let [{:keys [options arguments errors summary]} (cli/parse-opts args cli-options)]
    (cond
      (:help options)
      (exit 0 (usage summary))

      errors
      (exit 1 (str/join \newline errors))

      (empty? arguments)
      (exit 0 (usage summary))

      :else
      {:pattern (first arguments)
       :sources (rest arguments)})))

(defn -main
  [& args]
  (let [{:keys [pattern sources]} (parse-args args)
        pattern (g/parse-pattern pattern)]
    (doseq [source sources]
      ;; TODO support -R or similar
      (let [code (slurp (io/file source))]
        (doseq [m (g/find-codes code pattern)]
          (println (:match m)))))))
