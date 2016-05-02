## Introduction

A Clojure and ClojureScript library that wraps [parinfer.js](https://github.com/shaunlebron/parinfer) and [parinfer-jvm](https://github.com/oakmac/parinfer-jvm) so it is accessible on both platforms with the same functions. It supplies `paren-mode` and `indent-mode` functions, both of which take the text, the cursor's x position, and the line (the latter two are zero-based).

```clojure
(paren-mode "(+ 1\n1)" 0 0)
; => {:x 0, :text "(+ 1\n 1)"}

(indent-mode "(+ 1 1" 6 0)
; => {:x 6, :text "(+ 1 1)"}
```

Additionally, it provides a function called `add-indent` to add indentation where appropriate. It takes a map containing the text, a cursor position (a vector with two numbers representing the begin and end cursor, which are the same number if there is no selection), and the indent type. The possible indent types are :return, :forward, and :back, which should be supplied when the user types the return key, tab key, or shift+tab key respectively. Examples:

```clojure
(add-indent {:text "(+ 1 1\n)" :cursor-position [7 7] :indent-type :return})
; => {:cursor-position [10 10], :text "(+ 1 1\n   )"}

(add-indent {:text "(+ 1 1)\n(+ 1 1)" :cursor-position [0 10] :indent-type :forward})
{:cursor-position [0 19], :text "  (+ 1 1)\n  (+ 1 1)"}
```

## Licensing

All files that originate from this project are dedicated to the public domain. I would love pull requests, and will assume that they are also dedicated to the public domain.
