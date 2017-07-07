(set-env!
  :source-paths #{"src"}
  :resource-paths #{"src"}
  :dependencies '[[org.clojure/clojure "1.8.0" :scope "provided"]
                  [org.clojure/clojurescript "1.9.671" :scope "provided"]
                  [tag-soup "1.4.4"]
                  [org.clojars.oakes/parinfer "0.4.0"]
                  [cljsjs/parinfer "1.8.1-0"]]
  :repositories (conj (get-env :repositories)
                  ["clojars" {:url "https://clojars.org/repo/"
                              :username (System/getenv "CLOJARS_USER")
                              :password (System/getenv "CLOJARS_PASS")}]))

(task-options!
  pom {:project 'cross-parinfer
       :version "1.3.8-SNAPSHOT"
       :description "A library that wraps Parinfer for Clojure and ClojureScript"
       :url "https://github.com/oakes/cross-parinfer"
       :license {"Public Domain" "http://unlicense.org/UNLICENSE"}}
  push {:repo "clojars"})

(deftask local []
  (comp (pom) (jar) (install)))

(deftask deploy []
  (comp (pom) (jar) (push)))

