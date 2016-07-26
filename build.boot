(set-env!
  :resource-paths #{"src"}
  :dependencies '[[org.clojure/clojure "1.8.0"]
                  [org.clojure/clojurescript "1.8.51"]
                  [prismatic/schema "0.4.3"]
                  [tag-soup "1.3.3"]
                  [org.clojars.oakes/parinfer "0.4.0"]
                  [cljsjs/parinfer "1.8.1-0"]]
  :repositories (conj (get-env :repositories)
                  ["clojars" {:url "https://clojars.org/repo/"
                              :username (System/getenv "CLOJARS_USER")
                              :password (System/getenv "CLOJARS_PASS")}]))

(task-options!
  pom {:project 'cross-parinfer
       :version "1.1.11-SNAPSHOT"
       :description "A library that wraps Parinfer for Clojure and ClojureScript"
       :url "https://github.com/oakes/cross-parinfer"
       :license {"Public Domain" "http://unlicense.org/UNLICENSE"}}
  push {:repo "clojars"})

(deftask run-repl []
  (repl :init-ns 'cross-parinfer.core))

(deftask try []
  (comp (pom) (jar) (install)))

(deftask deploy []
  (comp (pom) (jar) (push)))

