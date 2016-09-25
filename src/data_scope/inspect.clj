(ns data-scope.inspect
  (:require [clojure.inspector :as inspector]))

(defn inspect-table [[f :as data]]
  (->> (cond
         (map? f) data
         (sequential? f) (map  #(into {} (map-indexed (fn [k v]
                                                        [(keyword (str k)) v])
                                                      %))
                               data))
       inspector/inspect-table))

(defn scope-inspect-tree [form] `(do ('~inspector/inspect-tree ~form) ~form))
(defn scope-inspect-table [form] `(do (~inspect-table ~form) ~form))
