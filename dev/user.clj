(ns user
  (:require [clojure.tools.namespace.repl :refer [refresh]]
            [clojure.walk :refer [postwalk]]
            [parcera.core :as parcera]
            [instaparse.core :as insta]
            #_
            [com.rpl.specter :as sp]

            [grape.core :as g]))

(def s
  (slurp "src/grape/core.clj"))

(def code
  (g/parse-code s))
