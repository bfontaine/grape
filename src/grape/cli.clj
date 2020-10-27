(ns grape.cli
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [clojure.tools.cli :as cli]
            [grape.core :as g])
  (:import [java.nio.file FileSystems Path]
           [java.io File IOException])
  (:gen-class))

(def cli-options
  ;; Implicit convention: lower-case options enable features; upper-case options disable them.
  [["-h" "--help" "Show this help and exit."]
   ["-v" "--version" "Show the version and exit."]
   ["-c" "--count" "Print the total matches count and exit."]
   ["-F" "--no-filenames" "Don't show the filenames when matching against multiple files."]
   ["-u" "--unindent" "Remove indentation when printing matches."]
   [nil "--inline" "Remove whitespaces and comment to show multi-lines matches on a single line."]
   [nil "--line-numbers MODE"
    (str
      "Control which line numbers are shown:"
      " only the first line of each match ('first'; default); all lines ('all'); or none ('none')."
      " This takes precedence over any alias.")
    :validate [#(contains? #{"first" "all" "none"} %) "Must be 'first', 'all', or 'none."]]
   ["-n" "--all-line-numbers" "Show all line numbers. Alias for --line-numbers all."]
   ["-N" "--no-line-numbers"
    "Don't show any line number. Alias for --line-numbers none. This takes precedence over --all-line-numbers."]
   [nil "--no-trailing-newlines" "Don't append a newline after each match. This is implicit if --inline is used."]])

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

(def ^:private stdin-pseudo-path "-")

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
      (let [[pattern & paths] arguments
            all-paths (if (empty? paths)
                        [stdin-pseudo-path]
                        (mapcat (fn [path]
                                  (if (= path stdin-pseudo-path)
                                    [stdin-pseudo-path]
                                    (list-clojure-files path)))
                                paths))]
        {:pattern               pattern
         :paths                 all-paths
         :count?                (:count options)
         :hide-filenames?       (:no-filenames options)
         :unindent?             (:unindent options)
         :inline?               (:inline options)
         :no-trailing-newlines? (:no-trailing-newlines options)
         :line-numbers          (cond
                                  (contains? options :line-numbers)
                                  (keyword (:line-numbers options))

                                  (:no-line-numbers options)
                                  :none

                                  (:all-line-numbers options)
                                  :all

                                  :else
                                  :first)}))))

(defn unindent-lines
  "Unindent lines. Return a sequence of lines to be later joined with '\\n'."
  [lines]
  (let [prefix-size (->> lines
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
                                 nil)
                         count)]
    (map (fn [line]
           (if (empty? line)
             line
             (subs line prefix-size)))
         lines)))

(defn prepend-line-numbers
  "Prepend line numbers to each line in a sequence. Lines are left-padded with spaces.
   The mode can be :first (prepend the first line only) or :all."
  [start-line mode lines]
  {:pre [(#{:first :all} mode)]}
  (let [first-line-only? (= mode :first)
        max-line         (if first-line-only?
                           start-line
                           (+ start-line (count lines)))
        width            (count (str max-line))
        fmt              (str "%" width "d:%s")
        ;; if :first-line-number?, prepend all lines but the first one with spaces to keep the same offset.
        ;; inc for the ':'.
        whitespaces      (when first-line-only?
                           (str/join "" (repeat (inc width) " ")))]
    (map-indexed (fn [i line]
                   (if (and first-line-only? (> i 0))
                     (str whitespaces line)
                     (format fmt (+ i start-line) line)))
                 lines)))

(defn split-lines
  "clojure.string/split-lines that preserves trailing newlines and carriage returns."
  [s]
  ;; Note: str/split-lines doesn't preserve trailing newlines: (count (str/split-lines "a\n")) == 1.
  (str/split s #"\n" -1))

(defn join-lines
  [s]
  (str/join "\n" s))

(defn- match-string
  [m {:keys [unindent? line-numbers] :as _options}]
  (let [whitespace-count (-> m :meta :start :column)
        whitespace       (apply str (repeat whitespace-count " "))
        s                (str whitespace (:match m))]
    (join-lines
      (cond->> (split-lines s)
               unindent?
               unindent-lines

               line-numbers
               (prepend-line-numbers (-> m :meta :start :row) line-numbers)))))

(defn- read-path
  [path]
  (if (= stdin-pseudo-path path)
    {:path "(stdin)"
     :code (try (slurp *in*)
                (catch IOException _ ""))}
    {:path path
     :code (slurp (io/file path))}))

(defn- count-matches
  "Return the total matches count of a pattern against a sequence of sources."
  [sources pattern]
  (reduce (fn [n source]
            (+ n (g/count-codes (:code source) pattern)))
          0
          sources))

(defn match-source!
  "Match a pattern against a source and print all matches."
  ;; Note: this function and the ones it uses are independent of the command-line interface: while the CLI may have
  ;;   default options (which can be disabled), this function have no default feature-- everything is enabled through
  ;;   flags.
  [{:keys [code path]} pattern {:keys [show-filename? trailing-newline?] :as options}]
  (let [matches (g/find-codes code pattern)]
    (when (and show-filename? (seq matches))
      (println (str path ":")))
    (doseq [m (g/find-codes code pattern options)]
      (println (match-string m options))
      (when trailing-newline?
        (println)))))

(defn -main
  [& args]
  (let [{:keys [exit-code exit-text
                pattern paths
                count? unindent? inline? hide-filenames? no-trailing-newlines?
                line-numbers]} (parse-args args)
        _       (when exit-code
                  (exit! exit-code exit-text))
        pattern (g/pattern pattern)
        sources (map read-path paths)
        options {:show-filename?    (and (not hide-filenames?)
                                         (< 1 (count sources)))
                 :unindent?         unindent?
                 :inline?           inline?
                 :line-numbers      line-numbers
                 :trailing-newline? (and
                                      (not inline?)
                                      (not no-trailing-newlines?))}]
    (if count?
      (println (count-matches sources pattern))
      (doseq [source sources]
        (match-source! source pattern options)))))