(ns data-scope.pprint-test
  (:require [clojure.test :refer :all]
            [clojure.edn :as edn]
            [data-scope.pprint :refer :all]))

(def counter (atom 0))
(defn bump [] (swap! counter inc))

(deftest scope-pprint-test
  (binding [*data-readers* {'ds/pp scope-pprint}]
    (testing "scope-pprint evaluates form only once"
      (let [data [:a 1 :b 2 :c 3]]
        (reset! counter 0)
        (eval (read-string (str "#ds/pp (do (" #'bump ") " data ")")))
        (is (= @counter 1))))))
      
(deftest scope-print-table-test
  (binding [*data-readers* {'ds/pt scope-print-table}]
    (testing "scope-print-table"
      (let [data [{:first-name "James" :last-name "Sofra" :age 36}
                  {:first-name "Ada" :last-name "Lovelace":age 201 }]]
        (reset! counter 0)
        (eval (read-string (str "#ds/pt (do (" #'bump ") " data ")")))
        (is (= @counter 1))))))
