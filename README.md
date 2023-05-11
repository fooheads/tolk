# tolk

tolk is a library for building interpreters for arbitraty edn/Clojure data structures.
The name *tolk* is the Swedish word for interpreter.

## Features

* Remove boilerplate when interpreting Clojure data.
* Good error handling (errors as data).
* Support for local variable resolving.
* Customizable branching/evaluation/locals.


## Quickstart

Add the following to your `deps.edn`:
```clojure
com.fooheads/tolk {:mvn/version "0.9.0"}
```
Require tolk in your namespace header:
```clojure
(ns math
  (:require [fooheads.tolk :as tolk]))
```

### Your first interpreter

Define the vars you want to make available to the interpreter:
```clojure
(def vars  {'plus + 'minus -})
```
Run the interpreter:
```clojure
(tolk/interpret vars '(plus (minus 8 2) 5)) ; => {:tolk/result 11}
```

### Your second interpreter (transformer)

You can also use tolk to transform data structures into other data
structures. This example takes a hiccup like data structure and turns it
into a more Clojure like data structure:

```clojure
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

;; => {:tolk/result (plus 30000 (minus 20000 10000))}
```


### Composing interpreters

Interpreters can be composed in a similar way as `comp`. Let's compose the
hiccy interpreter and the math interpreter.

```clojure
(ns compose
  (:require
    [fooheads.tolk :as tolk]
    [hiccy :refer [transform-hiccy]]
    [math :refer [interpret-math]]))


(def interpreters [interpret-math transform-hiccy])
(def interpret (tolk/comp-interpreters interpret-math transform-hiccy))

(interpret '[:PLUS "30 000" [:MINUS "20 000" "10 000"]])

;; => {:tolk/result 40000}
```

## Options

Options can be passed both to `interpreter` and to the function returned
by `interpreter`. The following options are available:

* `resolve-local`:
  A function that resolves a local variable. Can be a map. Defaults to `{}`.
* `fn-name?`:
  A predicate that determines if an element is a function name. Defaults to `symbol?`
* `local?`:
  A predicate that determines if an element is a local variable name. Defaults to `symbol?`
* `branch?`:
  A predicate that determines if an element represents a branch. Defaults to `coll?`
* `evaluate?`:
  A predicate that determines if an element should be evaluated.
  Defaults to true for an element that is `sequential?` and has a `fn?` or a `var?` as the first element.


## Concepts


### Error handling

tolk will always wrap the result in a map containing either a `:tolk/result`
key or `:tolk/error`. The result can be forced by using the `get!` function.

To induce an error in one of the interpreting functions, you can either
throw an exception (which will be converted to data) or
return a map containing the key `:tolk/error`.

If you for instance would call the math interpreter above with the
following expression,
```clojure
(interpret vars '(div (minus 8 2) 5))
```
it will return:
```clojure
{:tolk/error {:data {:resolve-local {}
                     :resolve-var {minus #<clojure.core$_@24af33a1>
                                   plus #<clojure.core$_PLUS_@2ad51ba4>}
                     :symbol div}
              :message :tolk/unresolved-symbol}}
```

You can also provide interpreter specific error messages. This variant of
the `plus` function can return a specific error.

```clojure
(defn plus [a b]
  (if (and (pos? a) (pos? b))
    (+ a b)
    {:tolk/error {:message :can-only-add-positive-numbers
                  :data {:a a :b b}}}))
```
But, as mentioned, you can also just throw exceptions:
```clojure
(defn plus [a b]
  (assert (and (pos? a) (pos? b) "Can only add positive numbers")
  (+ a b)
```
Both of these examples will cause tolk to return `{:tolk/error ,,,}`


### Branching

tolk can be configured with a `branch?` function that determines whether to recurse into the data at hand.
`branch?` is similar to what is passed to `clojure.zip/zipper`.

The default `branch?` function is `coll?` which means tolk will recurse into all types of collections, including maps.
In the first example, `list?` would yield the same results.


### Evaluation

By default, data that is `sequential?` and has a `fn?` or a `var?` as the first element will be evaluated. Evaluation boils down to looking up the function in vars and applying it with the args.


### Local variables

If data is encountered that is `local?`, the value will be looked up using
`resolve-local`. If it's not found, it will be looked up in `resolve-var`.
If not found in either of those, an error will be returned.

By default, `local?` is `symbol?`, which means that all symbols
not in function position will be considered a local.


## License

Ths code in this repo is distributed under the Eclipse Public License, the same as Clojure.

