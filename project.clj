(defproject cross-parinfer "1.5.1-SNAPSHOT"
  :description "A library that wraps Parinfer for Clojure and ClojureScript"
  :url "https://github.com/oakes/cross-parinfer"
  :license {:name "Public Domain"
            :url "http://unlicense.org/UNLICENSE"}
  :plugins [[lein-tools-deps "0.4.3"]]
  :middleware [lein-tools-deps.plugin/resolve-dependencies-with-deps-edn]
  :lein-tools-deps/config {:config-files [:install :user :project]}
  :profiles {:dev {:main cross-parinfer.core}})
