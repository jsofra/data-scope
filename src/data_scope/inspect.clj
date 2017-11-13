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

(def ds-i-font-size (atom 18))

(defn inspect-tree
  "A version of inspect-tree that allow control of the font size."
  ([data]
   (inspect-tree data @ds-i-font-size))
  ([data font-size]
   (let [frame         (javax.swing.JFrame. "Clojure Inspector")
         tree          (javax.swing.JTree. (inspector/tree-model data))
         reset-tree!   (fn [font font-size]
                         (doto tree
                           (.setRowHeight (+ font-size 10))
                           (.setFont (.deriveFont font (float font-size)))))
         font-listener (proxy [javax.swing.AbstractAction] []
                         (actionPerformed [event]
                           (let [font      (.getFont tree)
                                 font-size (case (.getActionCommand event)
                                             "+" (float (+ (.getSize font) 2))
                                             "-" (float (- (.getSize font) 2)))]
                             (reset! ds-i-font-size font-size)
                             (reset-tree! font font-size)
                             (.pack frame))))
         font+         (doto (javax.swing.JButton. "+")
                         (.addActionListener font-listener))
         font-         (doto (javax.swing.JButton. "-")
                         (.addActionListener font-listener))
         top-panel     (javax.swing.JPanel. (java.awt.FlowLayout.
                                             java.awt.FlowLayout/LEADING))
         panel         (javax.swing.JPanel. (java.awt.BorderLayout.))]
     (doto frame
       (.add (doto panel
               (.add (doto top-panel
                       (.add font+)
                       (.add font-))
                     java.awt.BorderLayout/PAGE_START)
               (.add (javax.swing.JScrollPane.
                      (reset-tree! (.getFont tree) font-size))
                     java.awt.BorderLayout/CENTER)))
       (.pack)
       (.setVisible true)))))

(defn scope-inspect-tree [form]
  `(let [form# ~form] (~inspect-tree form#) form#))

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
