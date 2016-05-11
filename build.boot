(set-env!
  :source-paths #{"src"}
  :resource-paths #{"resources"}
  :dependencies '[[adzerk/boot-cljs "1.7.228-1" :scope "test"]
                  [adzerk/boot-cljs-repl "0.1.10-SNAPSHOT" :scope "test"]
                  [adzerk/boot-reload "0.3.1" :scope "test"]
                  [pandeiro/boot-http "0.6.3-SNAPSHOT" :scope "test"]
                  ; project deps
                  [org.clojure/clojure "1.8.0"]
                  [org.clojure/clojurescript "1.8.51"]
                  [prismatic/schema "0.4.3"]
                  [tag-soup "1.3.1"]
                  [org.clojars.oakes/parinfer "0.4.0"]
                  [cljsjs/parinfer "1.8.1-0"]])

(require
  '[adzerk.boot-cljs :refer [cljs]]
  '[adzerk.boot-cljs-repl :refer [cljs-repl start-repl]]
  '[adzerk.boot-reload :refer [reload]]
  '[pandeiro.boot-http :refer [serve]])

(deftask run []
  (set-env! :source-paths #{"src" "test"})
  (comp (serve :dir "target/public")
        (watch)
        (reload)
        (cljs-repl)
        (cljs :source-map true :optimizations :none)))

(deftask build []
  (set-env! :source-paths #{"src"})
  (comp (cljs :optimizations :advanced)))
