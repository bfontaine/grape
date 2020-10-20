(ns grape.cli
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [clojure.tools.cli :as cli]
            [grape.core :as g])
  (:import [java.nio.file FileSystems]
           [java.io File])
  (:gen-class))

(def cli-options
  [["-h" "--help" "Show this help and exit."]
   ["-v" "--version" "Show the version and exit."]])

(defn- usage
  [options-summary]
  (str/join
    \newline
    ["Usage: grape [options] PATTERN [FILE [FILE ...]]"
     ""
     "Options:"
     options-summary
     ""]))

(defn- version
  "Return the current version."
  []
  (slurp (io/resource "GRAPE_VERSION")))

(defn- exit
  [code msg]
  (binding [*out* *err*]
    (println msg))
  (System/exit code))

(defn list-clojure-files
  [root]
  ;; https://clojuredocs.org/clojure.core/file-seq#example-59f3948ee4b0a08026c48c79
  (let [clj-matcher (.getPathMatcher (FileSystems/getDefault)
                                     "glob:*.clj{,s,c,x}")]
    (->> root
         io/file
         file-seq
         (filter (fn [^File f]
                   (and (.isFile f)
                        (.matches clj-matcher
                                  (.getFileName (.toPath f))))))
         (map #(.getAbsolutePath ^File %)))))

(defn parse-args
  [args]
  (let [{:keys [options arguments errors summary]} (cli/parse-opts args cli-options)]
    (cond
      (:help options)
      (exit 0 (usage summary))

      (:version options)
      (exit 0 (version))

      errors
      (exit 1 (str/join \newline errors))

      (empty? arguments)
      (exit 0 (usage summary))

      :else
      (let [[pattern & sources] arguments]
        {:pattern pattern
         :sources (mapcat list-clojure-files sources)}))))

(defn- print-match
  [m]
  ; 'dec' so we can (println whitespace s)
  ; instead of (println (str whitespace s))
  (let [whitespace-count (-> m :meta :start :column dec)
        whitespace       (apply str (repeat whitespace-count " "))]
    (println whitespace (:match m))))

(defn -main
  [& args]
  (let [{:keys [pattern sources]} (parse-args args)
        pattern (g/pattern pattern)]
    ;; TODO if multiple sources, print them before matches
    (doseq [source sources]
      (let [code (slurp (io/file source))]
        (doseq [m (g/find-codes code pattern)]
          (print-match m))))))
