(ns data-scope.pprint
  (:require [clojure.pprint :as pprint]))

(defn print-table [[f :as data]]
  (->> (cond
        (map? f) [data]
        (sequential? f) [(range (count (first data)))
                         (map #(into {} (map-indexed vector %)) data)])
      (apply pprint/print-table)))

(defn scope-pprint [form]
  (println form)
  `(let [form# ~form]
     ('~pprint/pprint form#)
     form#))

(defn scope-print-table [form]
  `(let [form# ~form]
     (~print-table form#)
     form#))
