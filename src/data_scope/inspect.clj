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

(defn inspect-tree
  "A version of inspect-tree that allow control of the font size."
  [data font-size]
  (let [tree (javax.swing.JTree. (inspector/tree-model data))]
    (doto (javax.swing.JFrame. "Clojure Inspector")
      (.add (javax.swing.JScrollPane.
             (doto tree
               (.setRowHeight (+ font-size 4))
               (.setFont (.deriveFont (.getFont tree) (float font-size))))))
      (.pack)
      (.setVisible true))))

(def ds-i-font-size (atom 18))

(defn scope-inspect-tree [form]
  `(let [form# ~form] (~inspect-tree form# @ds-i-font-size) form#))

(defn scope-inspect-tree-thread-last [form]
  `((fn [x#]
      (let [form# (->> x# ~form)]
        ('~inspector/inspect-tree form#)
        form#))))

(defn scope-inspect-tree-thread-first [form]
  `((fn [x#]
      (let [form# (-> x# ~form)]
        ('~inspector/inspect-tree form#)
        form#))))

(defn scope-inspect-table [form]
  `(let [form# ~form]
     (~inspect-table form#)
     form#))

(defn scope-inspect-table-thread-last [form]
  `((fn [x#]
      (let [form# (->> x# ~form)]
        (~inspect-table form#)
        form#))))

(defn scope-inspect-table-thread-first [form]
  `((fn [x#]
      (let [form# (-> x# ~form)]
        (~inspect-table form#)
        form#))))
