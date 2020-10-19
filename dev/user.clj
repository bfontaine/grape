(ns user
  (:require [clojure.tools.namespace.repl :refer [refresh]]
            [clojure.walk :refer [postwalk]]
            [parcera.core :as parcera]
            [grape.core :as g]
            [grape.cli :as cli]))

(def s
  (slurp "src/grape/core.clj"))

(def code
  (g/parse-code s))
