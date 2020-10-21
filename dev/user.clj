(ns user
  (:require [clojure.tools.namespace.repl :refer [refresh]]
            [clojure.java.io :as io]
            [clojure.walk :refer [postwalk]]
            [parcera.core :as parcera]
            [grape.core :as g]
            [grape.cli :as cli]
            [grape.impl.models :as m]
            [grape.impl.match :as match]
            [grape.impl.parsing :as p]))

(def s
  (slurp "src/grape/core.clj"))

(def code
  (g/parse-code s))
