(defproject cross-parinfer "1.0.3-SNAPSHOT"
  :description "A library that wraps Parinfer for Clojure and ClojureScript"
  :url "https://github.com/oakes/cross-parinfer"
  :license {:name "Public Domain"
            :url "http://unlicense.org/UNLICENSE"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.7.228"
                  :exclusions [org.clojure/tools.reader]]
                 [prismatic/schema "0.4.3"]
                 [tag-soup "1.1.5"]
                 [org.clojars.oakes/parinfer "0.4.0"]
                 [cljsjs/parinfer "1.8.1-0"]]
  :profiles {:uberjar {:prep-tasks ["compile" ["cljsbuild" "once"]]}}
  :javac-options ["-target" "1.6" "-source" "1.6" "-Xlint:-options"]
  :plugins [[lein-cljsbuild "1.1.2"]]
  :cljsbuild {:builds {:main {:source-paths ["src"]
                              :compiler {:output-to "resources/public/cross-parinfer.js"
                                         :optimizations :advanced
                                         :pretty-print false}
                              :jar true}}}
  :main cross-parinfer.core)
