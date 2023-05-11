(ns fooheads.tolk-test
  (:require
    [clojure.test :refer [deftest is testing]]
    [fooheads.stdlib :refer [throw-ex]]
    [fooheads.test :refer [should-be]]
    [fooheads.tolk :as tolk :refer [interpreter]]))


(def math-env
  {'plus (fn [x y]
           (if-not (and (number? x) (number? y))
             {:tolk/error {:message "plus: can only add numbers"
                           :data {:x x :y y}}}
             (+ x y)))
   'times (fn [x y]
            (if-not (and (number? x) (number? y))
              {:tolk/error {:message "times: can only multiply numbers"
                            :data {:x x :y y}}}
              (* x y)))
   'minus (fn [_ _] nil)

   'divide (fn [_x _y] (throw-ex "problem" {}))})


(defn- interpret-math
  ([expr]
   (interpret-math expr {}))
  ([expr locals]
   ((#'interpreter math-env) expr locals)))


(deftest interpreter-test
  (testing "primitives"
    (is (= {:tolk/result 1} (interpret-math 1)))
    (is (= {:tolk/result "foobar"} (interpret-math "foobar"))))

  (testing "locals"
    (is (= {:tolk/result 6}
           (interpret-math 'a {:resolve-local '{a 6}})))

    (testing "happy, keywords"
      (is (= {:tolk/result 13}
             (interpret-math '(plus :a :b) {:resolve-local {:a 6 :b 7}
                                            :local? keyword?}))))

    (testing "unresolved"
      (should-be {:tolk/error
                  {:message :tolk/unresolved-symbol
                   :data {:symbol 'a}}}
                 (interpret-math '(plus a b)))))

  (testing "vars"
    (is (= {:tolk/result (get math-env 'plus)}
           (interpret-math 'plus {}))))

  (testing "evaluate"
    (is (= {:tolk/result 3}
           (interpret-math '(plus 1 2))))

    (is (= {:tolk/result 8}
           (interpret-math '(plus 2 (times 3 2))))))

  (testing "branch"
    (is (= {:tolk/result [:div 6]}
           (interpret-math '[:div (times 3 2)])))

    (testing "stopping branching"
      (is (= {:tolk/result '[:div (times 3 2)]}
             (interpret-math '[:div (times 3 2)]
                             {:branch? (constantly false)})))))

  (testing "errors"
    (testing "| user defined errors"
      (is (= {:tolk/error {:message "plus: can only add numbers"
                           :data {:x :a :y 2}}}
             (interpret-math '(plus (times 2 (plus :a 2)) 2))))

      (is (= {:tolk/error {:message "times: can only multiply numbers"
                           :data {:x :b :y 3}}}
             (interpret-math '(plus (times :b (plus 2 1)) 2))))

      (is (= {:tolk/error {:message "plus: can only add numbers"
                           :data {:x 4, :y nil}}}
             (interpret-math '(plus 4 (minus 2 1))))))

    (testing "| tolk defined errors"
      (testing "| unresolved symbol"
        (should-be {:tolk/error {:message :tolk/unresolved-symbol
                                 :data {:symbol 'div}}}
                   (interpret-math '(plus 2 (div 1 2)))))

      (testing "| exception"
        (should-be
          {:tolk/error {:message :tolk/unexpected-error
                        :data {:message "problem"}}}
          (interpret-math '(plus 3 (divide 1 0))))))))

