(ns examples.exceptional
  (:require
    [fooheads.stdlib :refer [exceptional]]
    [fooheads.tolk :as tolk :refer [interpreter]]))


(def vars {'plus + 'minus -})
(def interpret! (exceptional (interpreter vars) :tolk/result tolk/get!))


(interpret! '(div 6 2)) ; throws exception

(ex-data *e)

