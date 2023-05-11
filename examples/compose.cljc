(ns compose
  (:require
    [fooheads.tolk :as tolk]
    [hiccy :refer [transform-hiccy]]
    [math :refer [interpret-math]]))


(def interpreters [interpret-math transform-hiccy])
(def interpret (tolk/comp-interpreters interpret-math transform-hiccy))

(interpret '[:PLUS "30 000" [:MINUS "20 000" "10 000"]])

