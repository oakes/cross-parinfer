(ns cross-parinfer.core
  (:require [clojure.string :as str]
            [tag-soup.core :as ts]
            #?(:cljs [cljsjs.parinfer]))
  #?(:clj (:import [com.oakmac.parinfer Parinfer ParinferResult])))

(defn paren-mode
  "Runs paren mode on the given text."
  [text x line]
  #?(:clj
     (let [res (try
                 (Parinfer/parenMode text (int x) (int line) nil false)
                 (catch Exception _
                   (Parinfer/parenMode text (int 0) (int 0) nil false)))]
       {:x (.-cursorX res) :text (.-text res)})

     :cljs
     (let [res (.parenMode js/parinfer text #js {:cursorLine line :cursorX x})]
       {:x (aget res "cursorX") :text (aget res "text")})))

(defn indent-mode
  "Runs indent mode on the given text."
  ([text x line]
   (indent-mode text x line false))
  ([text x line preview-cursor-scope?]
   #?(:clj
      (let [res (try
                  (Parinfer/indentMode text (int x) (int line) nil preview-cursor-scope?)
                  (catch Exception _
                    (Parinfer/indentMode text (int 0) (int 0) nil preview-cursor-scope?)))]
        {:x (.-cursorX res) :text (.-text res)})
 
      :cljs
      (let [res (.indentMode js/parinfer text
                  #js {:cursorLine line
                       :cursorX x
                       :previewCursorScope preview-cursor-scope?})]
        {:x (aget res "cursorX") :text (aget res "text")}))))

(defn mode
  "Runs the specified mode, which can be :paren, :indent, or :both."
  [mode-type text x line]
  (case mode-type
    :paren
    (paren-mode text x line)
    :indent
    (indent-mode text x line)
    :both
    (-> text (paren-mode x line) :text (indent-mode x line))))

(defn split-lines
  "Splits the string into lines."
  [s]
  (vec (.split s "\n" -1)))

(defn position->row-col
  "Converts a position to a row and column number."
  [text position]
  (let [text (subs text 0 position)
        last-newline (.lastIndexOf text "\n")
        row (count (re-seq #"\n" text))
        col (if (>= last-newline 0)
              (- position last-newline 1)
              position)]
    [row col]))

(defn row-col->position
  "Converts a row and column number to a position."
  [text row col]
  (let [all-lines (vec (split-lines text))
        lines (vec (take row all-lines))
        last-line (get all-lines row)
        lines (if (and last-line (>= (count last-line) col))
                (conj lines (subs last-line 0 col))
                lines)
        text (str/join "\n" lines)]
    (count text)))

(defn add-parinfer
  "Adds parinfer to the state."
  [mode-type state]
  (let [{:keys [cursor-position text]} state
        [start-pos end-pos] cursor-position
        [row col] (position->row-col text start-pos)
        result (mode mode-type text col row)]
    (if (not= start-pos end-pos)
      (assoc state :text (:text result))
      (let [pos (row-col->position (:text result) row (:x result))]
        (assoc state :text (:text result) :cursor-position [pos pos])))))

(defn indent-count [line]
  (->> line seq (take-while #(= % \space)) count))

(defn update-indent [diff lines line-num]
  (update
    lines
    line-num
    (fn [line]
      (let [[spaces code] (split-with #(= % \space) (seq line))
            spaces (if (pos? diff)
                     (concat spaces (repeat diff \space))
                     (drop (* -1 diff) spaces))]
        (str (str/join spaces) (str/join code))))))

(defn add-indent
  "Adds indent to the state."
  [state]
  (let [; get the values out of the state
        {:keys [text cursor-position indent-type]} state
        [start-pos end-pos] cursor-position
        ; find the start and end lines
        [start-line start-x] (position->row-col text start-pos)
        [end-line _] (position->row-col text end-pos)
        ; split the text into lines
        lines (split-lines text)
        ; use tag-soup to parse the text into tags
        tags (ts/code->tags text)
        ; calculate the new indent level
        new-indent-level (case indent-type
                           :return
                           (ts/indent-for-line tags (inc start-line))
                           :back
                           (ts/back-indent-for-line tags (inc start-line) (indent-count (get lines start-line)))
                           :forward
                           (ts/forward-indent-for-line tags (inc start-line) (indent-count (get lines start-line)))
                           :normal
                           start-x)
        ; apply the indentation change to the relevant lines
        lines (if (= indent-type :normal)
                (loop [lines lines
                       tags tags
                       line-num (inc start-line)]
                  (if-let [line (get lines line-num)]
                    (let [indent (ts/indent-for-line tags (inc line-num))
                          current-indent (indent-count line)]
                      (if (and (> indent 0)
                               (> indent start-x)
                               (not= indent current-indent))
                        (let [lines (update-indent (- indent current-indent) lines line-num)
                              text (str/join \newline lines)
                              tags (ts/code->tags text)]
                          (recur lines tags (inc line-num)))
                        lines))
                    lines))
                (let [old-indent-level (indent-count (get lines start-line))
                      diff (- new-indent-level old-indent-level)
                      diff (if (neg? diff)
                             (->> (seq (get lines start-line))
                                  (split-with #(= % \space))
                                  first
                                  (take (* -1 diff))
                                  count
                                  (* -1))
                             diff)]
                  (reduce (partial update-indent diff) lines
                    (range start-line (inc end-line)))))
        ; create a string with the new text in it
        text (str/join \newline lines)
        ; apply indent mode to the new text
        text (:text (indent-mode text new-indent-level start-line true))
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

