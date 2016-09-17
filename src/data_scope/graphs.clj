(ns data-scope.graphs
  (:require [rhizome.viz :as viz]))

(defn scope-graph [g]
  (viz/view-graph
   (keys g) g
   :node->descriptor (fn [n]
                       {:label n})))

(defn scope-tree [t]
  (viz/view-tree
   sequential? seq t
   :node->descriptor (fn [n]
                       {:label (when (not (sequential? n)) n)})))

(def scope-dot (comp viz/view-image viz/dot->image))
