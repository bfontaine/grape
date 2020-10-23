(ns grape.cli
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [clojure.tools.cli :as cli]
            [grape.core :as g])
  (:import [java.nio.file FileSystems Path]
           [java.io File])
  (:gen-class))

(def cli-options
  [["-h" "--help" "Show this help and exit."]
   ["-v" "--version" "Show the version and exit."]
   ["-c" "--count" "Print the number of matches."]])

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
  (str/trim (slurp (io/resource "GRAPE_VERSION"))))

(defn- exit
  [code msg]
  (binding [*out* *err*]
    (println msg))
  (System/exit code))

;; TODO test me
(defn list-clojure-files
  [root]
  ;; https://clojuredocs.org/clojure.core/file-seq#example-59f3948ee4b0a08026c48c79
  (let [clj-matcher (.getPathMatcher (FileSystems/getDefault)
                                     "glob:*.clj{,s,c,x}")
        root-file   (io/file root)
        absolute?   (-> root-file .toPath .isAbsolute)]
    (->> root-file
         file-seq
         (filter (fn [^File f]
                   (and (.isFile f)
                        (.matches clj-matcher
                                  (.getFileName (.toPath f))))))
         (map (fn [^File f]
                ;; Return relative (but normalized) paths if the root was given as a relative path; otherwise,
                ;; return absolute paths.
                (str (.normalize
                       ^Path (cond-> (.toPath f)
                                     absolute?
                                     (.toAbsolutePath)))))))))

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
      (let [[pattern & paths] arguments]
        {:pattern pattern
         :count?  (:count options)
         :paths   (mapcat list-clojure-files paths)}))))

(defn- match-string
  [m]
  (let [whitespace-count (-> m :meta :start :column)
        whitespace       (apply str (repeat whitespace-count " "))]
    (str whitespace (:match m))))

(defn- print-match
  [m]
  (println (match-string m)))

(defn- read-path
  [path]
  {:path path
   :code (slurp (io/file path))})

(defn -main
  [& args]
  (let [{:keys [pattern paths count?]} (parse-args args)
        pattern (g/pattern pattern)
        sources (map read-path paths)]
    (if count?
      (println (reduce (fn [n source]
                         (+ n (g/count-codes (:code source) pattern)))
                       0
                       sources))
      ;; TODO if multiple sources, print them before matches
      (doseq [source sources]
        (doseq [m (g/find-codes (:code source) pattern)]
          (print-match m))))))
