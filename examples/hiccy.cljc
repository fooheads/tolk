(ns hiccy
  (:require
    [clojure.string :as str]
    [fooheads.tolk :refer [interpreter]]))


(defn maybe-coerce [x]
  (if (string? x) (parse-long (str/replace x #" " "")) x))


(defn resolve-var [x]
  (let [sym (-> x name str/lower-case symbol)]
    (fn [& args]
      (apply list (into [sym] (map maybe-coerce args))))))


(def transform-hiccy (interpreter resolve-var {:fn-name? keyword?
                                               :evaluate? vector?}))


(transform-hiccy '[:PLUS "30 000" [:MINUS "20 000" "10 000"]])

