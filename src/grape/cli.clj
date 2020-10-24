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
   ["-c" "--count" "Print the number of matches."]
   ["-F" "--no-filenames" "Don't show the filenames when matching against multiple files."]
   ["-u" "--unindent" "Remove indentation when printing matches."]])

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

(defn- exit!
  [code msg]
  (binding [*out* *err*]
    (println msg))
  (System/exit code))

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
  {:post [(map? %)]}
  (let [{:keys [options arguments errors summary]} (cli/parse-opts args cli-options)]
    (cond
      (:help options)
      {:exit-code 0 :exit-text (usage summary)}

      (:version options)
      {:exit-code 0 :exit-text (version)}

      errors
      {:exit-code 1 :exit-text (str/join \newline errors)}

      (empty? arguments)
      {:exit-code 0 :exit-text (usage summary)}

      :else
      (let [[pattern & paths] arguments]
        {:pattern         pattern
         :count?          (:count options)
         :hide-filenames? (:no-filenames options)
         :unindent?       (:unindent options)
         :paths           (mapcat list-clojure-files paths)}))))

(defn unindent
  "Unindent a string."
  [s]
  ;; Note: str/split-lines doesn't preserve trailing newlines: (count (str/split-lines "a\n")) == 1.
  (let [lines                 (str/split s #"\n" -1)
        longest-common-prefix (->> lines
                                   ; ignore empty lines
                                   (remove empty?)
                                   (map #(re-find #"^\s*" %))
                                   (reduce (fn [prefix line-prefix]
                                             (cond
                                               (nil? prefix)
                                               line-prefix

                                               ; abc and abcdef -> abc
                                               (str/starts-with? line-prefix prefix)
                                               prefix
                                               ; abcdef and abc -> abc
                                               (str/starts-with? prefix line-prefix)
                                               line-prefix
                                               ; abcde and abcef -> abc
                                               :else
                                               (->> (map #(when (= %1 %2) %1) prefix line-prefix)
                                                    (take-while some?)
                                                    ; avoid an error on str/join if the sequence is empty
                                                    (cons "")
                                                    (str/join))))
                                           nil))
        common-prefix-size    (count longest-common-prefix)]
    (->> lines
         (map (fn [line]
                (if (empty? line)
                  line
                  (subs line common-prefix-size))))
         (str/join "\n"))))

(defn- match-string
  [m {:keys [unindent?] :as _options}]
  (let [whitespace-count (-> m :meta :start :column)
        whitespace       (apply str (repeat whitespace-count " "))]
    (cond-> (str whitespace (:match m))
            unindent?
            unindent)))

(defn- read-path
  [path]
  {:path path
   :code (slurp (io/file path))})

(defn- count-matches
  [sources pattern]
  (reduce (fn [n source]
            (+ n (g/count-codes (:code source) pattern)))
          0
          sources))

(defn match-source!
  [{:keys [code path]} pattern {:keys [show-filename?] :as options}]
  (let [matches (g/find-codes code pattern)]
    (when (and show-filename? (seq matches))
      (println (str path ":")))
    (doseq [m (g/find-codes code pattern)]
      (println (match-string m options)))))

(defn -main
  [& args]
  (let [{:keys [exit-code exit-text
                pattern paths
                count? hide-filenames? unindent?]} (parse-args args)
        _       (when exit-code
                  (exit! exit-code exit-text))
        pattern (g/pattern pattern)
        sources (map read-path paths)
        options {:show-filename? (and (not hide-filenames?)
                                      (< 1 (count sources)))
                 :unindent?      unindent?}]
    (if count?
      (println (count-matches sources pattern))
      (doseq [source sources]
        (match-source! source pattern options)))))