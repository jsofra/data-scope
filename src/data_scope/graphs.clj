(ns data-scope.graphs
  (:require [rhizome.viz :as viz]))

(defn view-graph [g]
  (viz/view-graph
   (keys g) g
   :node->descriptor (fn [n]
                       {:label n})))

(defn view-tree* [t branch? label?]
  (viz/view-tree
   branch? seq t
   :node->descriptor (fn [n] {:label (when (label? n) n)})))

(defn view-tree [t]
  (view-tree* t sequential? #(not (sequential? %))))

(defn view-trie [t]
  (view-tree* t list? vector?))

(def view-dot (comp viz/view-image viz/dot->image))

(defn scope [f form] `(do (~f ~form) ~form))

(defn scope-graph [form] (scope `view-graph form))
(defn scope-tree  [form] (scope `view-tree form))
(defn scope-trie  [form] (scope `view-trie form))
(defn scope-dot   [form] (scope `view-dot form))
