(set-env!
  :dependencies '[[org.clojars.oakes/boot-tools-deps "0.1.4.1" :scope "test"]]
  :repositories (conj (get-env :repositories)
                  ["clojars" {:url "https://clojars.org/repo/"
                              :username (System/getenv "CLOJARS_USER")
                              :password (System/getenv "CLOJARS_PASS")}]))

(require '[boot-tools-deps.core :refer [deps]])

(task-options!
  pom {:project 'cross-parinfer
       :version "1.4.4"
       :description "A library that wraps Parinfer for Clojure and ClojureScript"
       :url "https://github.com/oakes/cross-parinfer"
       :license {"Public Domain" "http://unlicense.org/UNLICENSE"}}
  push {:repo "clojars"})

(deftask local []
  (comp (deps) (pom) (jar) (install)))

(deftask deploy []
  (comp (deps) (pom) (jar) (push)))

