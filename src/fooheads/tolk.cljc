(ns fooheads.tolk
  "Tolk is an interpreter that can interpret Clojure data
  strutures and return whataever you define. It's purpose
  is to take away boilerplate code when interpreting or
  transforming data structures."
  (:require
    [fooheads.stdlib :as std]))


(defn error?
  "Test whether the interpreted result is an error"
  [x]
  (boolean (and (map? x) (:tolk/error x))))


(def success?
  "Test whether the interpreted result is a success"
  (complement error?))


(defn get!
  "Forces the value of an interpreted result. If an error occurred during
  interpretation, and exception will be thrown."
  [res]
  (if (error? res)
    (throw (ex-info (str "tolk: " (name (get-in res [:tolk/error :message]))) res))
    (:tolk/result res)))


(def ^:private default-resolve-local {})
(def ^:private default-fn-name? symbol?)
(def ^:private default-local? symbol?)
(def ^:private default-branch? coll?)


(defn- default-evaluate? [expr]
  (and (sequential? expr)
       (or (var? (first expr))
           (fn? (first expr)))))


(defn- return [result]
  (if (success? result)
    {:tolk/result result}
    result))


(defn- lookup [resolve-var resolve-local sym]
  (if-let [v (or (resolve-local sym) (resolve-var sym))]
    v
    {:tolk/error
     {:message :tolk/unresolved-symbol
      :data {:symbol sym :resolve-var resolve-var :resolve-local resolve-local}}}))


(defn- evaluate [unresolved-expr resolved-expr]
  (try
    (apply (first resolved-expr) (rest resolved-expr))

    (catch #?(:clj Exception :cljs js/Error) e
      {:tolk/error
       {:message :tolk/unexpected-error
        :data {:form unresolved-expr
               :call resolved-expr
               :message (ex-message e)
               :exception e}}})))


(defn interpreter
  "Returns a function that can interpret an arbitrary form. Useful for
  interpreting the same form in multiple different ways. The returned intepret
  function interprets an expression and returns a map with either :tolk/result
  or :tolk/error key and corresponding data.

  `resolve-var` is an ifn that takes x and returns the function represented
  by x, which means that `resolve-var` can be a function or map. To use an entire
  namespace, the result of `(ns-publics 'some.ns)` can be passed as `resolve-var`

  `opts` is a map which can contains one of the following keys:

  * `

  The README, tests and examples are the best way to see different usages, but
  here is a small example of a happy case. Given the expression:

  ```
  (def expr '(plus (minus 8 2) 5))
  ```

  we can make an interpreted calculator:

  ```
  (let [calculate (interpreter {'plus + 'minus -})]
    (calculate expr))

  ; => {:tolk/result 11}
  ```
  "
  ([resolve-var]
   (interpreter resolve-var {}))
  ([resolve-var opts]
   (let [global-opts opts]
     (letfn [(interpret-all
               [expressions opts]
               (reduce
                 (fn [results expr]
                   (let [res (interpret expr opts)]
                     (if (error? res)
                       (reduced res)
                       (conj results res))))
                 []
                 expressions))

             (interpret
               ([expr]
                (interpret expr {}))
               ([expr opts]
                (let [{:as opts
                       :keys [resolve-local fn-name? local? branch? evaluate?]
                       :or {resolve-local default-resolve-local
                            local? default-local?
                            branch? default-branch?
                            fn-name? default-fn-name?
                            evaluate? default-evaluate?}}
                      (merge global-opts opts)]
                  (return
                    (cond
                      (error? expr)
                      expr

                      (or (fn-name? expr) (local? expr))
                      (lookup resolve-var resolve-local expr)

                      (branch? expr)
                      (let [result (interpret-all expr opts)]
                        (if (error? result)
                          result
                          (let [result
                                (->>
                                  result
                                  (mapv :tolk/result)
                                  (std/into (std/empty expr)))]

                            (if (evaluate? result)
                              (evaluate expr result)
                              result))))

                      :else
                      expr)))))]

       interpret))))


(defn interpret
  "Interprets the specified `expr` by creating and running an `interpreter`.

  See `interpreter` for available options"
  ([resolve-var expr]
   (interpret resolve-var expr {}))
  ([resolve-var expr opts]
   (let [interpret (interpreter resolve-var opts)]
     (interpret expr opts))))


(defn comp-interpreters
  "Composes interpreters in a similar way as `clojure.core/comp`. Returns a
  chained interpreter.

  If there is an error anywhere in the interpreter chain, the interpretation
  chain is stopped and the error is returned."
  [& interpreters]
  (fn interpret-fn
   ([expr]
    (interpret-fn expr {}))
   ([expr opts]
    (let [res
          (reduce
            (fn [expr interpreter-fn]
              (let [res (interpreter-fn expr opts)]
                (if (success? res)
                  (get! res)
                  (reduced res))))
            expr
            (reverse interpreters))]
      (if (error? res)
        res
        {:tolk/result res})))))

