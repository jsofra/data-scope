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

(defn chart-data-dispatch [data chart]
  [(type (.getPlot chart)) (type data) (first (map type data))])

(defmulti chart-data #'chart-data-dispatch)

(defmethod chart-data
  [org.jfree.chart.plot.CategoryPlot java.lang.Iterable java.lang.Number]
  [data chart]
  (build-chart chart [data]))

(defmethod chart-data
  [org.jfree.chart.plot.CategoryPlot java.lang.Iterable java.lang.Iterable]
  [data chart]
  (build-chart chart data))

(defmethod chart-data
  [org.jfree.chart.plot.CategoryPlot clojure.lang.IPersistentMap clojure.lang.MapEntry]
  [data chart]
  (build-chart chart (keys data) (vals data)))

(defmethod chart-data
  [org.jfree.chart.plot.PiePlot clojure.lang.IPersistentMap clojure.lang.MapEntry]
  [data chart]
  (let [dataset (.. chart getPlot getDataset)]
    (doseq [[k v] data]
      (.setValue dataset (str k " " v) v)))
  chart)

(defn view-chart [data chart]
  (incanter/view (chart-data data chart)))

(defn scope-bar [form]
  `(do (view-chart ~form (empty-chart charts/bar-chart)) ~form))

(defn scope-line [form]
  `(do (view-chart ~form (empty-chart charts/line-chart)) ~form))

(defn scope-area [form]
  `(do (view-chart ~form (empty-chart charts/area-chart)) ~form))

(defn scope-stacked-area [form]
  `(do (view-chart ~form (empty-chart charts/stacked-area-chart)) ~form))

(defn scope-pie [form]
  `(do (view-chart ~form (empty-chart charts/pie-chart)) ~form))
