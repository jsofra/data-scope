(ns data-scope.eval-once-test
  (:require [clojure.test :refer :all]
            [clojure.edn :as edn]
            [data-scope.pprint :refer [scope-pprint scope-print-table]]
            [data-scope.charts :refer [scope histogram-scope]]
            [data-scope.inspect :refer [scope-inspect-table scope-inspect-tree]]))

(def counter (atom 0))
(defn bump [] (swap! counter inc))
(defn make-bumping-str [tagged-literal data]
  (str "#" (symbol tagged-literal) " (do (" #'bump ") " data ")"))

(defmacro def-eval-once-test [fname tagged-literal data]
  (let [test-name (symbol (str (symbol fname) "-eval-once-test"))]
    `(deftest ~test-name
       (binding [*data-readers* {(quote ~tagged-literal) ~fname}]
         (testing (str ~fname " evaluates form only once")
           (let [str# (make-bumping-str (quote ~tagged-literal) ~data)]
             (reset! counter 0)
             (eval (read-string str#))
             (is (= @counter 1))))))))

(def-eval-once-test
  scope-pprint
  ds/pp
  [:a 1 :b 2 :c 3])

(def-eval-once-test
  scope-print-table
  ds/pt
  [{:first-name "James" :last-name "Sofra" :age 36}
   {:first-name "Ada" :last-name "Lovelace":age 201 }])

