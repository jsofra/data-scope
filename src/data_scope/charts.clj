(ns data-scope.charts
  (:require [incanter.core :as incanter]
            [incanter.charts :as charts]))

(defn build-chart
  ([chart data]
   (build-chart chart data data))
  ([chart labels data]
   (reduce (fn [c [l d]]
             (charts/add-categories
              c (range) d
              :series-label (str l)))
           chart (map vector labels data))))

(defmacro empty-chart [chart-m]
  `(~chart-m [] []
    :x-label ""
    :y-label ""
    :legend true))

(defn chart-data-dispatch [data chart modifier]
  [(type (.getPlot chart)) (type data) (first (map type data))])

(defmulti chart-data #'chart-data-dispatch)

(defmethod chart-data
  [org.jfree.chart.plot.CategoryPlot java.lang.Iterable java.lang.Number]
  [data chart _]
  (build-chart chart [data]))

(defmethod chart-data
  [org.jfree.chart.plot.CategoryPlot java.lang.Iterable java.lang.Iterable]
  [data chart modifier]
  (build-chart chart (modifier data)))

(defmethod chart-data
  [org.jfree.chart.plot.CategoryPlot clojure.lang.IPersistentMap clojure.lang.MapEntry]
  [data chart modifier]
  (build-chart chart (keys data) (modifier (vals data))))

(defmethod chart-data
  [org.jfree.chart.plot.PiePlot clojure.lang.IPersistentMap clojure.lang.MapEntry]
  [data chart modifier]
  (let [dataset (.. chart getPlot getDataset)]
    (doseq [[k v] data :let [v (modifier v)]]
      (.setValue dataset (str k " " v) v)))
  chart)

(defn view-chart [data chart modifier]
  (incanter/view (chart-data data chart modifier)))

(defn scope [modifier empty-chart-fn form]
  `(do (view-chart ~form (empty-chart ~empty-chart-fn) ~modifier)
       ~form))

(def scope-ident (partial scope `identity))

(defn *-scope [modifier empty-chart-fn form]
  (scope `(partial mapv (partial apply ~modifier))
         empty-chart-fn form))

(defn bar-scope          [modifier form] (*-scope modifier `charts/bar-chart form))
(defn area-scope         [modifier form] (*-scope modifier `charts/area-chart form))
(defn stacked-area-scope [modifier form] (*-scope modifier `charts/stacked-area-chart form))
(defn pie-scope          [modifier form] (*-scope modifier `charts/pie-chart form))
(defn line-scope [modifier form]
  (scope `(fn [data#]
            [(mapv (partial apply ~modifier) data#)])
         `charts/line-chart form))

(defn scope-bar          [form] (scope-ident `charts/bar-chart form))
(defn scope-line         [form] (scope-ident `charts/line-chart form))
(defn scope-area         [form] (scope-ident `charts/area-chart form))
(defn scope-stacked-area [form] (scope-ident `charts/stacked-area-chart form))
(defn scope-pie          [form] (scope-ident `charts/pie-chart form))

(defn scope-bar-sum          [form] (bar-scope + form))
(defn scope-line-sum         [form] (line-scope + form))
(defn scope-area-sum         [form] (area-scope + form))
(defn scope-stacked-area-sum [form] (stacked-area-scope + form))
(defn scope-pie-sum          [form] (pie-scope + form))

(defn scope-bar-max          [form] (bar-scope max form))
(defn scope-line-max         [form] (line-scope max form))
(defn scope-area-max         [form] (area-scope max form))
(defn scope-stacked-area-max [form] (stacked-area-scope max form))
(defn scope-pie-max          [form] (pie-scope max form))

(defn scope-bar-min          [form] (bar-scope min form))
(defn scope-line-min         [form] (line-scope min form))
(defn scope-area-min         [form] (area-scope min form))
(defn scope-stacked-area-min [form] (stacked-area-scope min form))
(defn scope-pie-min          [form] (pie-scope min form))
