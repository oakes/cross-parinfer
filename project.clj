(defproject cross-parinfer "1.5.1-SNAPSHOT"
  :description "A library that wraps Parinfer for Clojure and ClojureScript"
  :url "https://github.com/oakes/cross-parinfer"
  :license {:name "Public Domain"
            :url "http://unlicense.org/UNLICENSE"}
  :repositories [["clojars" {:url "https://clojars.org/repo"
                             :sign-releases false}]]
  :profiles {:dev {:main cross-parinfer.core}})
