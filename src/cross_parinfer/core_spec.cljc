(ns cross-parinfer.core-spec
  (:require [cross-parinfer.core :as c]
            [tag-soup.core-spec]
            [clojure.spec :as s :refer [fdef]]))

(s/def ::x integer?)
(s/def ::text string?)
(s/def ::result (s/keys :req-un [::x ::text]))
(s/def ::cursor-position (s/tuple integer? integer?))
(s/def ::indent-type #{:return :forward :back :normal})
(s/def ::state (s/keys :req-un [::cursor-position ::text] :opt-un [::indent-type]))

(fdef c/paren-mode
  :args (s/cat :text string? :x integer? :line integer?)
  :ret ::result)

(fdef c/indent-mode
  :args (s/alt
          :three-args (s/cat :text string? :x integer? :line integer?)
          :four-args (s/cat :text string? :x integer? :line integer? :preview-cursor-scope? boolean?))
  :ret ::result)

(fdef c/mode
  :args (s/cat :mode-type keyword? :text string? :x integer? :line integer?)
  :ret ::result)

(fdef c/split-lines
  :args (s/cat :str string?)
  :ret (s/coll-of string?))

(fdef c/position->row-col
  :args (s/cat :text string? :position integer?)
  :ret (s/coll-of integer?))

(fdef c/row-col->position
  :args (s/cat :text string? :row integer? :col integer?)
  :ret integer?)

(fdef c/add-parinfer
  :args (s/cat :mode-type keyword? :state ::state)
  :ret ::state)

(fdef c/indent-count
  :args (s/cat :line string?)
  :ret integer?)

(fdef c/add-indent
  :args (s/cat :state ::state)
  :ret ::state)

