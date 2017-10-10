(ns data-scope.pprint
  (:require [clojure.pprint :as pprint]))

(defn print-table [[f :as data]]
  (->> (cond
        (map? f) [data]
        (sequential? f) [(range (count (first data)))
                         (map #(into {} (map-indexed vector %)) data)])
      (apply pprint/print-table)))

(defn scope-pprint [form]
  `(let [form# ~form]
     ('~pprint/pprint form#)
     form#))

(defn scope-pprint-thread-last [form]
  `((fn [x#]
      (let [form# (->> x# ~form)]
        ('~pprint/pprint form#)
        form#))))

(defn scope-pprint-thread-first [form]
  `((fn [x#]
      (let [form# (-> x# ~form)]
        ('~pprint/pprint form#)
        form#))))

(defn scope-print-table [form]
  `(let [form# ~form]
     (~print-table form#)
     form#))

(defn scope-print-table-thread-last [form]
  `((fn [x#]
      (let [form# (->> x# ~form)]
        (~print-table form#)
        form#))))

(defn scope-print-table-thread-first [form]
  `((fn [x#]
      (let [form# (-> x# ~form)]
        (~print-table form#)
        form#))))
