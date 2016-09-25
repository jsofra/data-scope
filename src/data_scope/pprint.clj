(ns data-scope.pprint)

(defn print-table [[f :as data]]
  (->> (cond
        (map? f) [data]
        (sequential? f) [(range (count (first data)))
                         (map #(into {} (map-indexed vector %)) data)])
      (apply clojure.pprint/print-table)))

(defn scope-pprint [form] `(do ('~clojure.pprint/pprint ~form) ~form))
(defn scope-print-table [form] `(do (~print-table ~form) ~form))
