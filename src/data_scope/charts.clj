(ns data-scope.charts
  (:require [incanter.core :as incanter]
            [incanter.charts :as charts]))

(defn apply-row-op [data op]
  (mapv #(apply op %) data))

(defn apply-col-op [data op]
  (apply mapv op data))

(defn var-identity [& args] args)

(defn op-applicator-dispatch [applicator data]
  [(type data) (first (map type data))])

(defmulti op-applicator #'op-applicator-dispatch)

(defmethod op-applicator
  :default
  [_ data]
  (fn [_] [[data]]))

(defmethod op-applicator
  [java.lang.Iterable java.lang.Iterable]
  [applicator data]
  (fn [op] [(applicator data op)]))

(defmethod op-applicator
  [clojure.lang.IPersistentMap clojure.lang.MapEntry]
  [applicator data]
  (fn [op] [(keys data) (applicator (vals data) op)]))

(defmacro empty-chart [chart-m title]
  `(~chart-m [] []
    :x-label ""
    :y-label ""
    :legend true
    :title (str ~title)))

(defn build-category-chart
  ([chart data]
   (build-category-chart chart data data))
  ([chart labels data]
   (reduce (fn [c [l d]]
             (charts/add-categories
              c (range) d
              :series-label (str (if (seq? l) (into [] l) l))))
           chart
           (map vector labels data))))

(defn build-pie-chart
  ([chart data]
   (let [data (flatten data)]
     (build-pie-chart chart (range (count data)) data)))
  ([chart labels data]
   (let [dataset (.. chart getPlot getDataset)]
     (doseq [[k v] (map vector labels data)]
       (.setValue dataset (str k " " v) v)))
   chart))

(defn view-chart
  [chart-builder empty-chart applicator op data post-application-fn]
  (incanter/view (apply chart-builder
                        empty-chart
                        (-> ((op-applicator applicator data) op)
                            post-application-fn))))

(defn ^:dynamic scope
  "Create a scope (data inspection) for a chart."
  [chart-builder empty-chart-fn
   applicator op form & {:keys [post-apply-fn
                                chart-modifier-fn
                                title-prefix]
                         :or   {post-apply-fn     identity
                                chart-modifier-fn identity
                                title-prefix ""}}]
  `(do (view-chart ~chart-builder
                   (-> (empty-chart
                        ~empty-chart-fn
                        ~(str (if (not (empty? title-prefix))
                                (str title-prefix " - ")
                                "")
                              form))
                       ~chart-modifier-fn)
                   ~applicator
                   ~op
                   ~form
                   ~post-apply-fn)
       ~form))

(defn category-chart-scope [& args]
  (apply scope (partial build-category-chart) args))

(defn pie-chart-scope [& args]
  (apply scope (partial build-pie-chart) args))


(defn vectorize-items [data] (mapv vector data))
(defn vectorize-last-items [data] [(mapv vector (last data))])

;; bar scopes

(defn scope-bar-row [form op & options]
  (apply category-chart-scope
         `charts/bar-chart apply-row-op op form options))

(defn scope-bar-col [form op & options]
  (apply category-chart-scope
         `charts/bar-chart apply-col-op op form options))

(defn scope-bar [form]
  (scope-bar-row form var-identity))

(defn scope-bar-sum [form]
  (scope-bar-row form +
                 :title-prefix "row sum"))

(defn scope-bar-sum* [form]
  (scope-bar-col form +
                 :title-prefix "column sum"
                 :post-apply-fn vectorize-last-items))

(defn scope-bar-max [form]
  (scope-bar-row form max
                 :title-prefix "row max"))

(defn scope-bar-max* [form]
  (scope-bar-col form max
                 :title-prefix "column max"
                 :post-apply-fn vectorize-last-items))

(defn scope-bar-min [form]
  (scope-bar-row form min
                 :title-prefix "row min"))

(defn scope-bar-min* [form]
  (scope-bar-col form min
                 :title-prefix "column min"
                 :post-apply-fn vectorize-last-items))


;; line scopes

(defn scope-line-row [form op & options]
  (apply category-chart-scope
         `charts/line-chart apply-row-op op form options))

(defn scope-line-col [form op & options]
  (apply category-chart-scope
         `charts/line-chart apply-col-op op form options))

(defn scope-line [form]
  (scope-line-row form var-identity))

(defn scope-line-sum [form]
  (scope-line-row form +
                  :title-prefix "row sum"
                  :post-apply-fn vectorize-items))

(defn scope-line-sum* [form]
  (scope-line-col form +
                  :title-prefix "column sum"
                  :post-apply-fn vectorize-items))

(defn scope-line-max [form]
  (scope-line-row form max
                  :title-prefix "row max"
                  :post-apply-fn vectorize-items))

(defn scope-line-max* [form]
  (scope-line-col form max
                  :title-prefix "column max"
                  :post-apply-fn vectorize-items))

(defn scope-line-min [form]
  (scope-line-row form min
                  :title-prefix "row min"
                  :post-apply-fn vectorize-items))

(defn scope-line-min* [form]
  (scope-line-col form min
                  :title-prefix "column min"
                  :post-apply-fn vectorize-items))


;; area scopes

(defn scope-area-row [form op & options]
  (apply category-chart-scope
         `charts/area-chart apply-row-op op form options))

(defn scope-area-col [form op & options]
  (apply category-chart-scope
         `charts/area-chart apply-col-op op form options))

(defn scope-area [form]
  (scope-area-row form var-identity
                  :chart-modifier-fn #(charts/set-alpha % 0.5)))

(defn scope-area-sum [form]
  (scope-area-row form +
                  :title-prefix "row sum"
                  :post-apply-fn vectorize-items))

(defn scope-area-sum* [form]
  (scope-area-col form +
                  :title-prefix "column sum"
                  :post-apply-fn vectorize-items))

(defn scope-area-max [form]
  (scope-area-row form max
                  :title-prefix "row max"
                  :post-apply-fn vectorize-items))

(defn scope-area-max* [form]
  (scope-area-col form max
                  :title-prefix "column max"
                  :post-apply-fn vectorize-items))

(defn scope-area-min [form]
  (scope-area-row form min
                  :title-prefix "row min"
                  :post-apply-fn vectorize-items))

(defn scope-area-min* [form]
  (scope-area-col form min
                  :title-prefix "column min"
                  :post-apply-fn vectorize-items))


;; stacked area scopes

(defn scope-stacked-area-row [form op & options]
  (apply category-chart-scope
         `charts/stacked-area-chart apply-row-op op form options))

(defn scope-stacked-area-col [form op & options]
  (apply category-chart-scope
         `charts/stacked-area-chart apply-col-op op form options))

(defn scope-stacked-area [form]
  (scope-stacked-area-row form var-identity))

(defn scope-stacked-area-sum [form]
  (scope-stacked-area-row form +
                          :title-prefix "row sum"
                          :post-apply-fn vectorize-items))

(defn scope-stacked-area-sum* [form]
  (scope-stacked-area-col form +
                          :title-prefix "column sum"
                          :post-apply-fn vectorize-items))

(defn scope-stacked-area-max [form]
  (scope-stacked-area-row form max
                          :title-prefix "row max"
                          :post-apply-fn vectorize-items))

(defn scope-stacked-area-max* [form]
  (scope-stacked-area-col form max
                          :title-prefix "column max"
                          :post-apply-fn vectorize-items))

(defn scope-stacked-area-min [form]
  (scope-stacked-area-row form min
                          :title-prefix "row min"
                          :post-apply-fn vectorize-items))

(defn scope-stacked-area-min* [form]
  (scope-stacked-area-col form min
                          :title-prefix "column min"
                          :post-apply-fn vectorize-items))


;; pie scopes

(defn scope-pie-row [form op & options]
  (apply pie-chart-scope
         `charts/pie-chart apply-row-op op form options))

(defn scope-pie-col [form op & options]
  (apply pie-chart-scope
         `charts/pie-chart apply-col-op op form options))

(defn seq-op-applicator [data op] (map op data))

(defn scope-pie [form]
  (pie-chart-scope `charts/pie-chart seq-op-applicator identity form))

(defn scope-pie-sum [form]
  (scope-pie-row form +
                 :title-prefix "row sum"))

(defn scope-pie-sum* [form]
  (scope-pie-col form +
                 :title-prefix "column sum"))

(defn scope-pie-max [form]
  (scope-pie-row form max
                 :title-prefix "row max"))

(defn scope-pie-max* [form]
  (scope-pie-col form max
                 :title-prefix "column max"))

(defn scope-pie-min [form]
  (scope-pie-row form min
                 :title-prefix "row min"))

(defn scope-pie-min* [form]
  (scope-pie-col form min
                 :title-prefix "column min"))

(comment

  ;; used to generate docs

  (def ^:dynamic *path* "/tmp/foo.png")
  (def ^:dynamic *tag* "#ds/b")

  (defn save-chart
    [chart-builder empty-chart applicator op data post-application-fn]
    (incanter/save (apply chart-builder
                          empty-chart
                          (-> ((op-applicator applicator data) op)
                              post-application-fn))
                   *path*))

  (defn save-scope
    "Create a scope (data inspection) for a chart."
    [chart-builder empty-chart-fn
     applicator op form & {:keys [post-apply-fn
                                  chart-modifier-fn
                                  title-prefix]
                           :or   {post-apply-fn     identity
                                  chart-modifier-fn identity
                                  title-prefix ""}}]
    `(do
       (println "``` clojure")
       (println "user>" *tag* '~form)
       (println "```")
       (println  (str "![\"\"](" *path* ")"))
       (println " ")

       (save-chart ~chart-builder
                   (-> (empty-chart
                        ~empty-chart-fn
                        ~(str (if (not (empty? title-prefix))
                                (str title-prefix " - ")
                                "")
                              form))
                       ~chart-modifier-fn)
                   ~applicator
                   ~op
                   ~form
                   ~post-apply-fn)
       ~form))

  (binding [scope save-scope]

    ;; bar

    (binding [*tag* "#ds/b"]
      (println " ")
      (println "#### `#ds/b`")
      (println " ")
      (binding [*path* "doc/bar_ns.png"]
        #ds/b (range 10))
      (binding [*path* "doc/bar_s_ns.png"]
        #ds/b [(range 10) [20 1 2 23 8 3 7 4 6 5] (reverse (range 4 14))])
      (binding [*path* "doc/bar_map_ns.png"]
        #ds/b {:a (range 10) :b [20 1 2 23 8 3 7 4 6 5] :c (reverse (range 4 14))}))

    (binding [*tag* "#ds/b-sum"]
      (println " ")
      (println "#### `#ds/b-sum` - row wise sum")
      (println " ")
      (binding [*path* "doc/bar_s_ns_sum.png"]
        #ds/b-sum [(range 10) [20 1 2 23 8 3 7 4 6 5] (reverse (range 4 14))])
      (binding [*path* "doc/bar_map_ns_sum.png"]
        #ds/b-sum {:a (range 10) :b [20 1 2 23 8 3 7 4 6 5] :c (reverse (range 4 14))}))

    (binding [*tag* "#ds/b-sum*"]
      (println " ")
      (println "#### `#ds/b-sum*` - column wise sum")
      (println " ")
      (binding [*path* "doc/bar_s_ns_sum_c.png"]
        #ds/b-sum* [(range 10) [20 1 2 23 8 3 7 4 6 5] (reverse (range 4 14))])
      (binding [*path* "doc/bar_map_ns_sum_c.png"]
        #ds/b-sum* {:a (range 10) :b [20 1 2 23 8 3 7 4 6 5] :c (reverse (range 4 14))}))

    (binding [*tag* "#ds/b-max"]
      (println " ")
      (println "#### `#ds/b-max` - row wise max")
      (println " ")
      (binding [*path* "doc/bar_s_ns_max.png"]
        #ds/b-max [(range 10) [20 1 2 23 8 3 7 4 6 5] (reverse (range 4 14))])
      (binding [*path* "doc/bar_map_ns_max.png"]
        #ds/b-max {:a (range 10) :b [20 1 2 23 8 3 7 4 6 5] :c (reverse (range 4 14))}))

    (binding [*tag* "#ds/b-max*"]
      (println " ")
      (println "#### `#ds/b-max*` - column wise max")
      (println " ")
      (binding [*path* "doc/bar_s_ns_max_c.png"]
        #ds/b-max* [(range 10) [20 1 2 23 8 3 7 4 6 5] (reverse (range 4 14))])
      (binding [*path* "doc/bar_map_ns_max_c.png"]
        #ds/b-max* {:a (range 10) :b [20 1 2 23 8 3 7 4 6 5] :c (reverse (range 4 14))}))

    (binding [*tag* "#ds/b-min"]
      (println " ")
      (println "#### `#ds/b-min` - row wise min")
      (println " ")
      (binding [*path* "doc/bar_s_ns_min.png"]
        #ds/b-min [(range 10) [20 1 2 23 8 3 7 4 6 5] (reverse (range 4 14))])
      (binding [*path* "doc/bar_map_ns_min.png"]
        #ds/b-min {:a (range 10) :b [20 1 2 23 8 3 7 4 6 5] :c (reverse (range 4 14))}))

    (binding [*tag* "#ds/b-min*"]
      (println " ")
      (println "#### `#ds/b-min*` - column wise min")
      (println " ")
      (binding [*path* "doc/bar_s_ns_min_c.png"]
        #ds/b-min* [(range 10) [20 1 2 23 8 3 7 4 6 5] (reverse (range 4 14))])
      (binding [*path* "doc/bar_map_ns_min_c.png"]
        #ds/b-min* {:a (range 10) :b [20 1 2 23 8 3 7 4 6 5] :c (reverse (range 4 14))}))

    ;; line

    (binding [*tag* "#ds/l"]
      (println " ")
      (println "#### `#ds/l`")
      (println " ")
      (binding [*path* "doc/line_ns.png"]
        #ds/l (range 10))
      (binding [*path* "doc/line_s_ns.png"]
        #ds/l [(range 10) [20 1 2 23 8 3 7 4 6 5] (reverse (range 4 14))])
      (binding [*path* "doc/line_map_ns.png"]
        #ds/l {:a (range 10) :b [20 1 2 23 8 3 7 4 6 5] :c (reverse (range 4 14))}))

    (binding [*tag* "#ds/l-sum"]
      (println " ")
      (println "#### `#ds/l-sum` - row wise sum")
      (println " ")
      (binding [*path* "doc/line_s_ns_sum.png"]
        #ds/l-sum [(range 10) [20 1 2 23 8 3 7 4 6 5] (reverse (range 4 14))])
      (binding [*path* "doc/line_map_ns_sum.png"]
        #ds/l-sum {:a (range 10) :b [20 1 2 23 8 3 7 4 6 5] :c (reverse (range 4 14))}))

    (binding [*tag* "#ds/l-sum*"]
      (println " ")
      (println "#### `#ds/l-sum*` - column wise sum")
      (println " ")
      (binding [*path* "doc/line_s_ns_sum_c.png"]
        #ds/l-sum* [(range 10) [20 1 2 23 8 3 7 4 6 5] (reverse (range 4 14))])
      (binding [*path* "doc/line_map_ns_sum_c.png"]
        #ds/l-sum* {:a (range 10) :b [20 1 2 23 8 3 7 4 6 5] :c (reverse (range 4 14))}))

    (binding [*tag* "#ds/l-max"]
      (println " ")
      (println "#### `#ds/l-max` - row wise max")
      (println " ")
      (binding [*path* "doc/line_s_ns_max.png"]
        #ds/l-max [(range 10) [20 1 2 23 8 3 7 4 6 5] (reverse (range 4 14))])
      (binding [*path* "doc/line_map_ns_max.png"]
        #ds/l-max {:a (range 10) :b [20 1 2 23 8 3 7 4 6 5] :c (reverse (range 4 14))}))

    (binding [*tag* "#ds/l-max*"]
      (println " ")
      (println "#### `#ds/l-max*` - column wise max")
      (println " ")
      (binding [*path* "doc/line_s_ns_max_c.png"]
        #ds/l-max* [(range 10) [20 1 2 23 8 3 7 4 6 5] (reverse (range 4 14))])
      (binding [*path* "doc/line_map_ns_max_c.png"]
        #ds/l-max* {:a (range 10) :b [20 1 2 23 8 3 7 4 6 5] :c (reverse (range 4 14))}))

    (binding [*tag* "#ds/l-min"]
      (println " ")
      (println "#### `#ds/l-min` - row wise min")
      (println " ")
      (binding [*path* "doc/line_s_ns_min.png"]
        #ds/l-min [(range 10) [20 1 2 23 8 3 7 4 6 5] (reverse (range 4 14))])
      (binding [*path* "doc/line_map_ns_min.png"]
        #ds/l-min {:a (range 10) :b [20 1 2 23 8 3 7 4 6 5] :c (reverse (range 4 14))}))

    (binding [*tag* "#ds/l-min*"]
      (println " ")
      (println "#### `#ds/l-min*` - column wise min")
      (println " ")
      (binding [*path* "doc/line_s_ns_min_c.png"]
        #ds/l-min* [(range 10) [20 1 2 23 8 3 7 4 6 5] (reverse (range 4 14))])
      (binding [*path* "doc/line_map_ns_min_c.png"]
        #ds/l-min* {:a (range 10) :b [20 1 2 23 8 3 7 4 6 5] :c (reverse (range 4 14))}))

    ;; area

    (binding [*tag* "#ds/a"]
      (println " ")
      (println "#### `#ds/a`")
      (println " ")
      (binding [*path* "doc/area_ns.png"]
        #ds/a (range 10))
      (binding [*path* "doc/area_s_ns.png"]
        #ds/a [(range 10) [20 1 2 23 8 3 7 4 6 5] (reverse (range 4 14))])
      (binding [*path* "doc/area_map_ns.png"]
        #ds/a {:a (range 10) :b [20 1 2 23 8 3 7 4 6 5] :c (reverse (range 4 14))}))

    (binding [*tag* "#ds/a-sum"]
      (println " ")
      (println "#### `#ds/a-sum` - row wise sum")
      (println " ")
      (binding [*path* "doc/area_s_ns_sum.png"]
        #ds/a-sum [(range 10) [20 1 2 23 8 3 7 4 6 5] (reverse (range 4 14))])
      (binding [*path* "doc/area_map_ns_sum.png"]
        #ds/a-sum {:a (range 10) :b [20 1 2 23 8 3 7 4 6 5] :c (reverse (range 4 14))}))

    (binding [*tag* "#ds/a-sum*"]
      (println " ")
      (println "#### `#ds/a-sum*` - column wise sum")
      (println " ")
      (binding [*path* "doc/area_s_ns_sum_c.png"]
        #ds/a-sum* [(range 10) [20 1 2 23 8 3 7 4 6 5] (reverse (range 4 14))])
      (binding [*path* "doc/area_map_ns_sum_c.png"]
        #ds/a-sum* {:a (range 10) :b [20 1 2 23 8 3 7 4 6 5] :c (reverse (range 4 14))}))

    (binding [*tag* "#ds/a-max"]
      (println " ")
      (println "#### `#ds/a-max` - row wise max")
      (println " ")
      (binding [*path* "doc/area_s_ns_max.png"]
        #ds/a-max [(range 10) [20 1 2 23 8 3 7 4 6 5] (reverse (range 4 14))])
      (binding [*path* "doc/area_map_ns_max.png"]
        #ds/a-max {:a (range 10) :b [20 1 2 23 8 3 7 4 6 5] :c (reverse (range 4 14))}))

    (binding [*tag* "#ds/a-max*"]
      (println " ")
      (println "#### `#ds/a-max*` - column wise max")
      (println " ")
      (binding [*path* "doc/area_s_ns_max_c.png"]
        #ds/a-max* [(range 10) [20 1 2 23 8 3 7 4 6 5] (reverse (range 4 14))])
      (binding [*path* "doc/area_map_ns_max_c.png"]
        #ds/a-max* {:a (range 10) :b [20 1 2 23 8 3 7 4 6 5] :c (reverse (range 4 14))}))

    (binding [*tag* "#ds/a-min"]
      (println " ")
      (println "#### `#ds/a-min` - row wise min")
      (println " ")
      (binding [*path* "doc/area_s_ns_min.png"]
        #ds/a-min [(range 10) [20 1 2 23 8 3 7 4 6 5] (reverse (range 4 14))])
      (binding [*path* "doc/area_map_ns_min.png"]
        #ds/a-min {:a (range 10) :b [20 1 2 23 8 3 7 4 6 5] :c (reverse (range 4 14))}))

    (binding [*tag* "#ds/a-min*"]
      (println " ")
      (println "#### `#ds/a-min*` - column wise min")
      (println " ")
      (binding [*path* "doc/area_s_ns_min_c.png"]
        #ds/a-min* [(range 10) [20 1 2 23 8 3 7 4 6 5] (reverse (range 4 14))])
      (binding [*path* "doc/area_map_ns_min_c.png"]
        #ds/a-min* {:a (range 10) :b [20 1 2 23 8 3 7 4 6 5] :c (reverse (range 4 14))}))

    ;; stacked-area

    (binding [*tag* "#ds/sa"]
      (println " ")
      (println "#### `#ds/sa`")
      (println " ")
      (binding [*path* "doc/stacked-area_ns.png"]
        #ds/sa (range 10))
      (binding [*path* "doc/stacked-area_s_ns.png"]
        #ds/sa [(range 10) [20 1 2 23 8 3 7 4 6 5] (reverse (range 4 14))])
      (binding [*path* "doc/stacked-area_map_ns.png"]
        #ds/sa {:a (range 10) :b [20 1 2 23 8 3 7 4 6 5] :c (reverse (range 4 14))}))

    (binding [*tag* "#ds/sa-sum"]
      (println " ")
      (println "#### `#ds/sa-sum` - row wise sum")
      (println " ")
      (binding [*path* "doc/stacked-area_s_ns_sum.png"]
        #ds/sa-sum [(range 10) [20 1 2 23 8 3 7 4 6 5] (reverse (range 4 14))])
      (binding [*path* "doc/stacked-area_map_ns_sum.png"]
        #ds/sa-sum {:a (range 10) :b [20 1 2 23 8 3 7 4 6 5] :c (reverse (range 4 14))}))

    (binding [*tag* "#ds/sa-sum*"]
      (println " ")
      (println "#### `#ds/sa-sum*` - column wise sum")
      (println " ")
      (binding [*path* "doc/stacked-area_s_ns_sum_c.png"]
        #ds/sa-sum* [(range 10) [20 1 2 23 8 3 7 4 6 5] (reverse (range 4 14))])
      (binding [*path* "doc/stacked-area_map_ns_sum_c.png"]
        #ds/sa-sum* {:a (range 10) :b [20 1 2 23 8 3 7 4 6 5] :c (reverse (range 4 14))}))

    (binding [*tag* "#ds/sa-max"]
      (println " ")
      (println "#### `#ds/sa-max` - row wise max")
      (println " ")
      (binding [*path* "doc/stacked-area_s_ns_max.png"]
        #ds/sa-max [(range 10) [20 1 2 23 8 3 7 4 6 5] (reverse (range 4 14))])
      (binding [*path* "doc/stacked-area_map_ns_max.png"]
        #ds/sa-max {:a (range 10) :b [20 1 2 23 8 3 7 4 6 5] :c (reverse (range 4 14))}))

    (binding [*tag* "#ds/sa-max*"]
      (println " ")
      (println "#### `#ds/sa-max*` - column wise max")
      (println " ")
      (binding [*path* "doc/stacked-area_s_ns_max_c.png"]
        #ds/sa-max* [(range 10) [20 1 2 23 8 3 7 4 6 5] (reverse (range 4 14))])
      (binding [*path* "doc/stacked-area_map_ns_max_c.png"]
        #ds/sa-max* {:a (range 10) :b [20 1 2 23 8 3 7 4 6 5] :c (reverse (range 4 14))}))

    (binding [*tag* "#ds/sa-min"]
      (println " ")
      (println "#### `#ds/sa-min` - row wise min")
      (println " ")
      (binding [*path* "doc/stacked-area_s_ns_min.png"]
        #ds/sa-min [(range 10) [20 1 2 23 8 3 7 4 6 5] (reverse (range 4 14))])
      (binding [*path* "doc/stacked-area_map_ns_min.png"]
        #ds/sa-min {:a (range 10) :b [20 1 2 23 8 3 7 4 6 5] :c (reverse (range 4 14))}))

    (binding [*tag* "#ds/sa-min*"]
      (println " ")
      (println "#### `#ds/sa-min*` - column wise min")
      (println " ")
      (binding [*path* "doc/stacked-area_s_ns_min_c.png"]
        #ds/sa-min* [(range 10) [20 1 2 23 8 3 7 4 6 5] (reverse (range 4 14))])
      (binding [*path* "doc/stacked-area_map_ns_min_c.png"]
        #ds/sa-min* {:a (range 10) :b [20 1 2 23 8 3 7 4 6 5] :c (reverse (range 4 14))}))

    ;; pie

    (binding [*tag* "#ds/p"]
      (println " ")
      (println "#### `#ds/p`")
      (println " ")
      (binding [*path* "doc/pie_ns.png"]
        #ds/p (range 10))
      (binding [*path* "doc/pie_s_ns.png"]
        #ds/p [(range 2) [8 1 2] [2 4]])
      (binding [*path* "doc/pie_map_ns.png"]
        #ds/p {:a 10 :b 2 :c 6}))

    (binding [*tag* "#ds/p-sum"]
      (println " ")
      (println "#### `#ds/p-sum` - row wise sum")
      (println " ")
      (binding [*path* "doc/pie_s_ns_sum.png"]
        #ds/p-sum [(range 10) [20 1 2 23 8 3 7 4 6 5] (reverse (range 4 14))])
      (binding [*path* "doc/pie_map_ns_sum.png"]
        #ds/p-sum {:a (range 10) :b [20 1 2 23 8 3 7 4 6 5] :c (reverse (range 4 14))}))

    (binding [*tag* "#ds/p-sum*"]
      (println " ")
      (println "#### `#ds/p-sum*` - column wise sum")
      (binding [*path* "doc/pie_s_ns_sum_c.png"]
        #ds/p-sum* [(range 10) [20 1 2 23 8 3 7 4 6 5] (reverse (range 4 14))])
      (binding [*path* "doc/pie_map_ns_sum_c.png"]
        #ds/p-sum* {:a (range 10) :b [20 1 2 23 8 3 7 4 6 5] :c (reverse (range 4 14))}))

    (binding [*tag* "#ds/p-max"]
      (println " ")
      (println "#### `#ds/p-max` - row wise max")
      (println " ")
      (binding [*path* "doc/pie_s_ns_max.png"]
        #ds/p-max [(range 10) [20 1 2 23 8 3 7 4 6 5] (reverse (range 4 14))])
      (binding [*path* "doc/pie_map_ns_max.png"]
        #ds/p-max {:a (range 10) :b [20 1 2 23 8 3 7 4 6 5] :c (reverse (range 4 14))}))

    (binding [*tag* "#ds/p-max*"]
      (println " ")
      (println "#### `#ds/p-max*` - column wise max")
      (println " ")
      (binding [*path* "doc/pie_s_ns_max_c.png"]
        #ds/p-max* [(range 10) [20 1 2 23 8 3 7 4 6 5] (reverse (range 4 14))])
      (binding [*path* "doc/pie_map_ns_max_c.png"]
        #ds/p-max* {:a (range 10) :b [20 1 2 23 8 3 7 4 6 5] :c (reverse (range 4 14))}))

    (binding [*tag* "#ds/p-min"]
      (println " ")
      (println "#### `#ds/p-min` - row wise min")
      (println " ")
      (binding [*path* "doc/pie_s_ns_min.png"]
        #ds/p-min [(range 10) [20 1 2 23 8 3 7 4 6 5] (reverse (range 4 14))])
      (binding [*path* "doc/pie_map_ns_min.png"]
        #ds/p-min {:a (range 10) :b [20 1 2 23 8 3 7 4 6 5] :c (reverse (range 4 14))}))

    (binding [*tag* "#ds/p-min*"]
      (println " ")
      (println "#### `#ds/p-min*` - column wise min")
      (println " ")
      (binding [*path* "doc/pie_s_ns_min_c.png"]
        #ds/p-min* [(range 10) [20 1 2 23 8 3 7 4 6 5] (reverse (range 4 14))])
      (binding [*path* "doc/pie_map_ns_min_c.png"]
        #ds/p-min* {:a (range 10) :b [20 1 2 23 8 3 7 4 6 5] :c (reverse (range 4 14))}))

    )

  )
