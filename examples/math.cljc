(ns math
  (:require
    [fooheads.tolk :refer [interpret interpreter]]))


(def vars  {'plus + 'minus -})


(interpret vars '(plus (minus 8 2) 5))  ; => {:tolk/result 11}
(interpret vars '(div (minus 8 2) 5))   ; returns error data
;; => {:tolk/error {:data {:scope {,,,}}}))
;;                         :symbol div)
;;                  :message :tolk/unresolved-symbol))



;;;
;;; Use interpreter to bind vars (and options)
;;; Kind of partial, but does some of the work up front
;;;

(def interpret-math (interpreter vars))


(interpret-math '(plus (minus 8 2) 5))  ; => {:tolk/result 11}
(interpret-math '(div (minus 8 2) 5))   ; returns error data
;; => {:tolk/error {:data {:scope {,,,}}}))
;;                         :symbol div)
;;                  :message :tolk/unresolved-symbol))


