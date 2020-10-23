(defproject bfontaine/grape "0.3.0-SNAPSHOT"
  :description "Syntax-aware Grep-like for Clojure code"
  :url "https://github.com/bfontaine/grape"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url  "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.clojure/tools.cli "0.4.2"]
                 [carocad/parcera "0.11.5"]
                 ;; per https://github.com/carocad/parcera
                 [org.antlr/antlr4-runtime "4.7.1"]]
  :main grape.cli
  :repl-options {:init-ns user}
  :deploy-repositories [["snapshots" {:url           "https://repo.clojars.org"
                                      :username      :env/clojars_username
                                      :password      :env/clojars_password
                                      :sign-releases false}]
                        ["releases" {:url           "https://repo.clojars.org"
                                     :username      :env/clojars_username
                                     :password      :env/clojars_password
                                     :sign-releases false}]]
  :profiles {:uberjar      {:aot         :all
                            :global-vars {*assert* false}
                            :jvm-opts    ["-Dclojure.compiler.direct-linking=true"
                                          "-Dclojure.spec.skip-macros=true"]
                            :main        grape.cli}
             :native-image {:dependencies [[borkdude/clj-reflector-graal-java11-fix "0.0.1-graalvm-20.2.0"]]}
             :dev          {:source-paths ["dev"]
                            :global-vars  {*warn-on-reflection* true}
                            :dependencies [[org.clojure/tools.namespace "0.2.11"]]}})
