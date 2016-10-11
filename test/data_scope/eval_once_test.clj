(ns data-scope.eval-once-test
  (:require [clojure.test :refer :all]
            [clojure.edn :as edn]
            [data-scope.pprint :refer :all]
            [data-scope.graphs :refer :all]
            [data-scope.charts :refer :all]
            [data-scope.inspect :refer :all]))

(def counter (atom 0))
(defn bump [] (swap! counter inc))
(defn make-bumping-str [tagged-literal data]
  (str "#" (symbol tagged-literal) " (do (" #'bump ") " data ")"))

(defn no-op [& args] nil)

(defmacro def-eval-once-test [tagged-literal fname data]
  (let [test-name (symbol (str (symbol fname) "-eval-once-test"))]
    `(deftest ~test-name
       (binding [*data-readers* {(quote ~tagged-literal) ~fname}]
         (testing (str ~fname " evaluates form only once")
           (let [str# (make-bumping-str (quote ~tagged-literal) ~data)]
             (reset! counter 0)
             (with-redefs [clojure.inspector/inspect-table no-op
                           clojure.inspector/inspect-tree no-op
                           clojure.pprint/pprint no-op
                           clojure.pprint/print-table no-op
                           data-scope.graphs/view-dot no-op
                           data-scope.graphs/view-graph no-op
                           data-scope.graphs/view-tree* no-op
                           data-scope.charts/view-chart no-op]
               (eval (read-string str#)))
             (is (= @counter 1))))))))


(def-eval-once-test ds/b scope-bar [1 2])
(def-eval-once-test ds/b-sum scope-bar-sum {:a 1 :b 2})
(def-eval-once-test ds/b-sum* scope-bar-sum* {:a 1 :b 2})
(def-eval-once-test ds/b-max scope-bar-max {:a 1 :b 2})
(def-eval-once-test ds/b-max* scope-bar-max* {:a 1 :b 2})
(def-eval-once-test ds/b-min scope-bar-min {:a 1 :b 2})
(def-eval-once-test ds/b-min* scope-bar-min* {:a 1 :b 2})

(def-eval-once-test ds/l scope-line {:a 1 :b 2})
(def-eval-once-test ds/l-sum scope-line-sum {:a 1 :b 2})
(def-eval-once-test ds/l-sum* scope-line-sum* {:a 1 :b 2})
(def-eval-once-test ds/l-max scope-line-max {:a 1 :b 2})
(def-eval-once-test ds/l-max* scope-line-max* {:a 1 :b 2})
(def-eval-once-test ds/l-min scope-line-min {:a 1 :b 2})
(def-eval-once-test ds/l-min* scope-line-min* {:a 1 :b 2})

(def-eval-once-test ds/a scope-area {:a 1 :b 2})
(def-eval-once-test ds/a-sum scope-area-sum {:a 1 :b 2})
(def-eval-once-test ds/a-sum* scope-area-sum* {:a 1 :b 2})
(def-eval-once-test ds/a-max scope-area-max {:a 1 :b 2})
(def-eval-once-test ds/a-max* scope-area-max* {:a 1 :b 2})
(def-eval-once-test ds/a-min scope-area-min {:a 1 :b 2})
(def-eval-once-test ds/a-min* scope-area-min* {:a 1 :b 2})

(def-eval-once-test ds/sa scope-stacked-area {:a 1 :b 2})
(def-eval-once-test ds/sa-sum scope-stacked-area-sum {:a 1 :b 2})
(def-eval-once-test ds/sa-sum* scope-stacked-area-sum* {:a 1 :b 2})
(def-eval-once-test ds/sa-max scope-stacked-area-max {:a 1 :b 2})
(def-eval-once-test ds/sa-max* scope-stacked-area-max* {:a 1 :b 2})
(def-eval-once-test ds/sa-min scope-stacked-area-min {:a 1 :b 2})
(def-eval-once-test ds/sa-min* scope-stacked-area-min* {:a 1 :b 2})

(def-eval-once-test ds/p scope-pie {:a 1 :b 2})
(def-eval-once-test ds/p-sum scope-pie-sum {:a 1 :b 2})
(def-eval-once-test ds/p-sum* scope-pie-sum* {:a 1 :b 2})
(def-eval-once-test ds/p-max scope-pie-max {:a 1 :b 2})
(def-eval-once-test ds/p-max* scope-pie-max* {:a 1 :b 2})
(def-eval-once-test ds/p-min scope-pie-min {:a 1 :b 2})
(def-eval-once-test ds/p-min* scope-pie-min* {:a 1 :b 2})

(def-eval-once-test ds/graph scope-graph {:a [:b :c] :b [:c] :c [:a]})
(def-eval-once-test ds/tree scope-tree {:a 1 :b 2})
(def-eval-once-test ds/trie scope-trie {:a 1 :b 2})
(def-eval-once-test ds/dot scope-dot {:a 1 :b 2})

(def-eval-once-test ds/pp scope-pprint {:a 1 :b 2})
(def-eval-once-test ds/pt scope-print-table [{:a 1 :b 2} {:a 3 :b 4}])

(def-eval-once-test ds/i scope-inspect-tree {:a 1 :b 2})
(def-eval-once-test ds/it scope-inspect-table [{:a 1 :b 2} {:a 3 :b 4}])
