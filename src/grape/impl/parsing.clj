(ns grape.impl.parsing
  "Internal parsing utilities."
  (:require [parcera.core :as parcera]
            [clojure.walk :refer [postwalk]]
            [grape.impl.models :as m]))

(defn parse-code
  "Parse code. Options are passed to parcera.core/ast."
  ([code]
   (parse-code code nil))
  ([code options]
   ; parcera/ast takes options as [code & {:as options}]
   (apply parcera/ast code (mapcat vec options))))

(defn unparse-code
  "Unparse code."
  ([ast]
   (unparse-code ast nil))
  ([ast {:keys [inline?]}]
   (parcera/code
     (cond->> ast
              inline?
              m/compact-whitespaces))))