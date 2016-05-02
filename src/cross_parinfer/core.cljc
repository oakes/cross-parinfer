(ns cross-parinfer.core
  (:require [clojure.string :as str]
            [tag-soup.core :as ts]
            [schema.core :refer [maybe either Any Str Int Keyword Bool]
             #?@(:clj [:as s])]
            #?(:cljs [parinfer.core]))
  #?(:cljs (:require-macros [schema.core :as s])
     :clj (:import [com.oakmac.parinfer Parinfer ParinferResult])))

(s/defn paren-mode :- {Keyword Any}
  "Runs paren mode on the given text."
  [text :- Str
   x :- Int
   line :- Int]
  (let [res #?(:clj (try
                      (Parinfer/parenMode text (int x) (int line) nil false)
                      (catch Exception _
                        (Parinfer/parenMode text (int 0) (int 0) nil false)))
               :cljs (.parenMode js/parinfer text
                       #js {:cursorLine line :cursorX x}))]
    {:x (.-cursorX res) :text (.-text res)}))

(s/defn indent-mode :- {Keyword Any}
  "Runs indent mode on the given text."
  [text :- Str
   x :- Int
   line :- Int]
  (let [res #?(:clj (try
                      (Parinfer/indentMode text (int x) (int line) nil false)
                      (catch Exception _
                        (Parinfer/indentMode text (int 0) (int 0) nil false)))
                    :cljs (.indentMode js/parinfer text
                            #js {:cursorLine line :cursorX x}))]
    {:x (.-cursorX res) :text (.-text res)}))

(s/defn split-lines :- [Str]
  "Splits the string into lines."
  [s :- Str]
  (vec (.split s "\n" -1)))

(s/defn position->row-col :- [Int]
  "Converts a position to a row and column number."
  [text :- Str
   position :- Int]
  (let [text (subs text 0 position)
        last-newline (.lastIndexOf text "\n")
        row (count (re-seq #"\n" text))
        col (if (>= last-newline 0)
              (- position last-newline 1)
              position)]
    [row col]))

(s/defn row-col->position :- Int
  "Converts a row and column number to a position."
  [text :- Str
   row :- Int
   col :- Int]
  (let [all-lines (vec (split-lines text))
        lines (vec (take row all-lines))
        last-line (get all-lines row)
        lines (if (and last-line (>= (count last-line) col))
                (conj lines (subs last-line 0 col))
                lines)
        text (str/join "\n" lines)]
    (count text)))

(s/defn add-indent :- {Keyword Any}
  "Adds indent to the relevant line(s)."
  [state :- {Keyword Any}]
  (let [[start-pos end-pos] (:cursor-position state)
        text (:text state)
        [start-line start-x] (position->row-col text start-pos)
        [end-line _] (position->row-col text end-pos)
        lines-to-change (range start-line (inc end-line))
        lines (split-lines text)
        old-indent-level (->> (get lines start-line) seq (take-while #(= % \space)) count)
        tags (ts/str->tags text)
        new-indent-level (case (:indent-type state)
                           :return
                           (ts/indent-for-line tags start-line)
                           :back
                           (ts/back-indent-for-line tags start-line old-indent-level)
                           :forward
                           (ts/forward-indent-for-line tags start-line old-indent-level))
        indent-change (- new-indent-level old-indent-level)
        indent-change (if (neg? indent-change)
                        (->> (seq (get lines start-line))
                             (split-with #(= % \space))
                             first
                             (take (* -1 indent-change))
                             count
                             (* -1))
                        indent-change)
        lines (reduce
                (fn [lines line-to-change]
                  (update
                    lines
                    line-to-change
                    (fn [line]
                      (let [[spaces code] (split-with #(= % \space) (seq line))
                            spaces (if (pos? indent-change)
                                     (concat spaces (repeat indent-change \space))
                                     (drop (* -1 indent-change) spaces))]
                        (str (str/join spaces) (str/join code))))))
                lines
                lines-to-change)
        text (str/join \newline lines)
        text (if (= :return (:indent-type state))
               text
               (:text (indent-mode text start-x start-line)))
        lines (split-lines text)]
    {:cursor-position
     (if (= start-pos end-pos)
       (let [pos (row-col->position text start-line new-indent-level)]
         [pos pos])
       [(row-col->position text start-line 0)
        (row-col->position text end-line (count (get lines end-line)))])
     :text text}))
