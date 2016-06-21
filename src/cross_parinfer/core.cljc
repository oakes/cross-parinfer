(ns cross-parinfer.core
  (:require [clojure.string :as str]
            [tag-soup.core :as ts]
            [schema.core :refer [maybe either Any Str Int Keyword Bool]
             #?@(:clj [:as s])]
            #?(:cljs [cljsjs.parinfer]))
  #?(:cljs (:require-macros [schema.core :as s])
     :clj (:import [com.oakmac.parinfer Parinfer ParinferResult])))

(s/defn paren-mode :- {Keyword Any}
  "Runs paren mode on the given text."
  [text :- Str
   x :- Int
   line :- Int]
  #?(:clj
     (let [res (try
                 (Parinfer/parenMode text (int x) (int line) nil false)
                 (catch Exception _
                   (Parinfer/parenMode text (int 0) (int 0) nil false)))]
       {:x (.-cursorX res) :text (.-text res)})

     :cljs
     (let [res (.parenMode js/parinfer text #js {:cursorLine line :cursorX x})]
       {:x (aget res "cursorX") :text (aget res "text")})))

(s/defn indent-mode :- {Keyword Any}
  "Runs indent mode on the given text."
  [text :- Str
   x :- Int
   line :- Int]
  #?(:clj
     (let [res (try
                 (Parinfer/indentMode text (int x) (int line) nil false)
                 (catch Exception _
                   (Parinfer/indentMode text (int 0) (int 0) nil false)))]
       {:x (.-cursorX res) :text (.-text res)})

     :cljs
     (let [res (.indentMode js/parinfer text #js {:cursorLine line :cursorX x})]
       {:x (aget res "cursorX") :text (aget res "text")})))

(s/defn mode :- {Keyword Any}
  "Runs the specified mode, which can be :paren, :indent, or :both."
  [mode-type :- Keyword
   text :- Str
   x :- Int
   line :- Int]
  (case mode-type
    :paren
    (paren-mode text x line)
    :indent
    (indent-mode text x line)
    :both
    (-> text (indent-mode x line) :text (paren-mode x line))))

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

(s/defn add-parinfer :- {Keyword Any}
  "Adds parinfer to the state."
  [mode-type :- Keyword
   state :- {Keyword Any}]
  (let [{:keys [cursor-position text]} state
        [start-pos end-pos] cursor-position
        [row col] (position->row-col text start-pos)
        result (mode mode-type text col row)]
    (if (not= start-pos end-pos)
      (assoc state :text (:text result))
      (let [pos (row-col->position (:text result) row (:x result))]
        (assoc state :text (:text result) :cursor-position [pos pos])))))

(s/defn add-indent :- {Keyword Any}
  "Adds indent to the state."
  [state :- {Keyword Any}]
  (let [; get the values out of the state
        {:keys [text cursor-position indent-type]} state
        [start-pos end-pos] cursor-position
        ; find the start and end lines and calculate which lines need to be indented
        [start-line start-x] (position->row-col text start-pos)
        [end-line _] (position->row-col text end-pos)
        lines-to-change (range start-line (inc end-line))
        ; split the text into lines and find the old indent level
        lines (split-lines text)
        old-indent-level (->> (get lines start-line) seq (take-while #(= % \space)) count)
        ; use tag-soup to parse the text into tags and decide how much we need to indent
        tags (ts/code->tags text)
        new-indent-level (case indent-type
                           :return
                           (ts/indent-for-line tags (inc start-line))
                           :back
                           (ts/back-indent-for-line tags (inc start-line) old-indent-level)
                           :forward
                           (ts/forward-indent-for-line tags (inc start-line) old-indent-level))
        ; calculate how much to change the current indent
        indent-change (- new-indent-level old-indent-level)
        indent-change (if (neg? indent-change)
                        (->> (seq (get lines start-line))
                             (split-with #(= % \space))
                             first
                             (take (* -1 indent-change))
                             count
                             (* -1))
                        indent-change)
        ; apply the indentation change to the relevant lines
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
        ; create a string with the new text in it
        text (str/join \newline lines)
        ; apply indent mode to the new text if necessary
        text (if (= :return indent-type)
               text
               (:text (indent-mode text start-x start-line)))
        ; split the new text into lines so we can figure out the new cursor position
        lines (split-lines text)]
    ; return the new cursor position and text
    {:cursor-position
     (if (= start-pos end-pos)
       (let [pos (row-col->position text start-line new-indent-level)]
         [pos pos])
       [(row-col->position text start-line 0)
        (row-col->position text end-line (count (get lines end-line)))])
     :text text}))
