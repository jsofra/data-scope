(ns data-scope.charts
  (:require [incanter.core :as incanter]
            [incanter.charts :as charts]

            [clojure.core.cache :as cache]))

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

(defn set-chart-data
  [chart-builder chart data applicator op post-application-fn]
  (.. chart getCategoryPlot getDataset clear)
  (apply chart-builder chart
         (-> ((op-applicator applicator data) op)
             post-application-fn)))

(defn add-chart-watch!
  [data-ref chart-builder chart applicator op post-application-fn]
  (let [watch-key (keyword (str "ds-chart-watcher-" (java.util.UUID/randomUUID)))]
    (println "Watching chart with watch -" watch-key)
    (add-watch data-ref watch-key
               (fn [_ _ _ data]
                 (set-chart-data chart-builder chart data applicator op post-application-fn)))))

(def tagged-expr-cache
  (atom (cache/lru-cache-factory {})))

(defn reset-expr-cache! []
  (reset! tagged-expr-cache (cache/lru-cache-factory {})))

(defn set-expr-cache! [cache-factory & opts]
  (swap! tagged-expr-cache #(apply cache-factory % opts)))

(defn update-data!
  [cached-data new-data accumulating?]
  (if accumulating?
    (swap! cached-data conj new-data)
    (reset! cached-data new-data)))

(defn chart-title [title-prefix expr]
  (if-not (empty? title-prefix)
    (str title-prefix " - " expr)
    (str expr)))

(def ds-print-length (atom 10))
(def ds-print-level (atom 5))

(defn ^:dynamic ^:private scope
  "Create a scope (data inspection) for a chart."
  [chart-builder empty-chart-fn
   applicator op form & {:keys [post-apply-fn
                                chart-modifier-fn
                                title-prefix
                                persist?
                                accumulating?]
                         :or   {post-apply-fn     identity
                                chart-modifier-fn identity
                                title-prefix      ""
                                persist?          false
                                accumulating?     false}}]
  `(binding [*print-length* @ds-print-length
             *print-level*  @ds-print-level]
     (let [form#      ~form
           title#     (chart-title ~title-prefix '~form)
           expr-hash# (hash '~form)]
       (if (and ~persist?
                (cache/has? @tagged-expr-cache expr-hash#))
         (do
           (let [[cached-chart# cached-data#] (cache/lookup
                                               @tagged-expr-cache
                                               expr-hash#)]
             (set-chart-data ~chart-builder
                             cached-chart# (update-data! cached-data#
                                                         form#
                                                         ~accumulating?)
                             ~applicator ~op ~post-apply-fn))
           (swap! tagged-expr-cache cache/hit expr-hash#))
         (let [chart# (~chart-modifier-fn (empty-chart ~empty-chart-fn title#))
               data#  (if (instance? clojure.lang.IRef form#)
                        (do
                          (add-chart-watch! form# ~chart-builder chart#
                                            ~applicator ~op ~post-apply-fn)
                          @form#)
                        form#)
               data# (if ~accumulating? [data#] data#)]
           (swap! tagged-expr-cache cache/miss expr-hash# [chart# (atom data#)])
           (view-chart ~chart-builder chart# ~applicator ~op data# ~post-apply-fn)))
       form#)))

(defn category-chart-scope [& args]
  (apply scope (partial build-category-chart) args))

(defn pie-chart-scope [& args]
  (apply scope (partial build-pie-chart) args))


(defn vectorize-items [data] (mapv vector data))
(defn vectorize-last-items [data] [(mapv vector (last data))])


(defn p-and-a [name]
  `(do
     (defn ~(symbol (str name "-p")) [~'form]
       (~name ~'form :persist? true))
     (defn ~(symbol (str name "-a")) [~'form]
       (~name ~'form
        :persist? true
        :accumulating? true))))

(defmacro def-p-and-a [& names]
  `(do ~@(map p-and-a names)))


;; bar scopes

(defn scope-bar-row [form op & options]
  (apply category-chart-scope
         `charts/bar-chart apply-row-op op form options))

(defn scope-bar-col [form op & options]
  (apply category-chart-scope
         `charts/bar-chart apply-col-op op form options))

(defn scope-bar [form & options]
  (apply scope-bar-row form var-identity options))

(defn scope-bar-sum [form & options]
  (apply scope-bar-row form + :title-prefix "row sum" options))

(defn scope-bar-sum* [form & options]
  (apply scope-bar-col form +
         :title-prefix "column sum"
         :post-apply-fn vectorize-last-items
         options))

(defn scope-bar-max [form & options]
  (apply scope-bar-row form max
         :title-prefix "row max"
         options))

(defn scope-bar-max* [form & options]
  (apply scope-bar-col form max
         :title-prefix "column max"
         :post-apply-fn vectorize-last-items
         options))

(defn scope-bar-min [form & options]
  (apply scope-bar-row form min
         :title-prefix "row min"
         options))

(defn scope-bar-min* [form & options]
  (apply scope-bar-col form min
         :title-prefix "column min"
         :post-apply-fn vectorize-last-items
         options))

(def-p-and-a
  scope-bar
  scope-bar-sum scope-bar-sum*
  scope-bar-max scope-bar-max*
  scope-bar-min scope-bar-min*)

;; line scopes

(defn scope-line-row [form op & options]
  (apply category-chart-scope
         `charts/line-chart apply-row-op op form options))

(defn scope-line-col [form op & options]
  (apply category-chart-scope
         `charts/line-chart apply-col-op op form options))

(defn scope-line [form & options]
  (apply scope-line-row form var-identity options))

(defn scope-line-sum [form & options]
  (apply scope-line-row form +
         :title-prefix "row sum"
         :post-apply-fn vectorize-items
         options))

(defn scope-line-sum* [form & options]
  (apply scope-line-col form +
         :title-prefix "column sum"
         :post-apply-fn vectorize-items
         options))

(defn scope-line-max [form & options]
  (apply scope-line-row form max
         :title-prefix "row max"
         :post-apply-fn vectorize-items
         options))

(defn scope-line-max* [form & options]
  (apply scope-line-col form max
         :title-prefix "column max"
         :post-apply-fn vectorize-items
         options))

(defn scope-line-min [form & options]
  (apply scope-line-row form min
         :title-prefix "row min"
         :post-apply-fn vectorize-items
         options))

(defn scope-line-min* [form & options]
  (apply scope-line-col form min
         :title-prefix "column min"
         :post-apply-fn vectorize-items
         options))

(def-p-and-a
  scope-line
  scope-line-sum scope-line-sum*
  scope-line-max scope-line-max*
  scope-line-min scope-line-min*)

;; area scopes

(defn scope-area-row [form op & options]
  (apply category-chart-scope
         `charts/area-chart apply-row-op op form options))

(defn scope-area-col [form op & options]
  (apply category-chart-scope
         `charts/area-chart apply-col-op op form options))

(defn scope-area [form & options]
  (apply scope-area-row form var-identity
         :chart-modifier-fn #(charts/set-alpha % 0.5)
         options))

(defn scope-area-sum [form & options]
  (apply scope-area-row form +
         :title-prefix "row sum"
         :post-apply-fn vectorize-items
         options))

(defn scope-area-sum* [form & options]
  (apply scope-area-col form +
         :title-prefix "column sum"
         :post-apply-fn vectorize-items
         options))

(defn scope-area-max [form & options]
  (apply scope-area-row form max
         :title-prefix "row max"
         :post-apply-fn vectorize-items
         options))

(defn scope-area-max* [form & options]
  (apply scope-area-col form max
         :title-prefix "column max"
         :post-apply-fn vectorize-items
         options))

(defn scope-area-min [form & options]
  (apply scope-area-row form min
         :title-prefix "row min"
         :post-apply-fn vectorize-items
         options))

(defn scope-area-min* [form & options]
  (apply scope-area-col form min
         :title-prefix "column min"
         :post-apply-fn vectorize-items
         options))

(def-p-and-a
  scope-area
  scope-area-sum scope-area-sum*
  scope-area-max scope-area-max*
  scope-area-min scope-area-min*)

;; stacked area scopes

(defn scope-stacked-area-row [form op & options]
  (apply category-chart-scope
         `charts/stacked-area-chart apply-row-op op form options))

(defn scope-stacked-area-col [form op & options]
  (apply category-chart-scope
         `charts/stacked-area-chart apply-col-op op form options))

(defn scope-stacked-area [form & options]
  (apply scope-stacked-area-row form var-identity options))

(defn scope-stacked-area-sum [form & options]
  (apply scope-stacked-area-row form +
         :title-prefix "row sum"
         :post-apply-fn vectorize-items
         options))

(defn scope-stacked-area-sum* [form & options]
  (apply scope-stacked-area-col form +
         :title-prefix "column sum"
         :post-apply-fn vectorize-items
         options))

(defn scope-stacked-area-max [form & options]
  (apply scope-stacked-area-row form max
         :title-prefix "row max"
         :post-apply-fn vectorize-items
         options))

(defn scope-stacked-area-max* [form & options]
  (apply scope-stacked-area-col form max
         :title-prefix "column max"
         :post-apply-fn vectorize-items
         options))

(defn scope-stacked-area-min [form & options]
  (apply scope-stacked-area-row form min
         :title-prefix "row min"
         :post-apply-fn vectorize-items
         options))

(defn scope-stacked-area-min* [form & options]
  (apply scope-stacked-area-col form min
         :title-prefix "column min"
         :post-apply-fn vectorize-items
         options))

(def-p-and-a
  scope-stacked-area
  scope-stacked-area-sum scope-stacked-area-sum*
  scope-stacked-area-max scope-stacked-area-max*
  scope-stacked-area-min scope-stacked-area-min*)

;; pie scopes

(defn scope-pie-row [form op & options]
  (apply pie-chart-scope
         `charts/pie-chart apply-row-op op form options))

(defn scope-pie-col [form op & options]
  (apply pie-chart-scope
         `charts/pie-chart apply-col-op op form options))

(defn seq-op-applicator [data op] (map op data))

(defn scope-pie [form & options]
  (apply pie-chart-scope
         `charts/pie-chart seq-op-applicator identity form options))

(defn scope-pie-sum [form & options]
  (apply scope-pie-row form +
         :title-prefix "row sum"
         options))

(defn scope-pie-sum* [form & options]
  (apply scope-pie-col form +
         :title-prefix "column sum"
         options))

(defn scope-pie-max [form & options]
  (apply scope-pie-row form max
         :title-prefix "row max"
         options))

(defn scope-pie-max* [form & options]
  (apply scope-pie-col form max
         :title-prefix "column max"
         options))

(defn scope-pie-min [form & options]
  (apply scope-pie-row form min
         :title-prefix "row min"
         options))

(defn scope-pie-min* [form & options]
  (apply scope-pie-col form min
         :title-prefix "column min"
         options))

(def-p-and-a
  scope-pie
  scope-pie-sum scope-pie-sum*
  scope-pie-max scope-pie-max*
  scope-pie-min scope-pie-min*)

;; histograms

(defn histogram-chart
  ([title density? data]
   (charts/histogram (flatten data)
                     :x-label ""
                     :title title
                     :density density?))
  ([title density? _ data]
   (histogram-chart title density? data)))

(defn view-histogram
  [applicator op data title density?]
  (incanter/view
   (apply histogram-chart title density?
          ((op-applicator applicator data) op))))

(defn histogram-scope
  "Create a scope (data inspection) for a chart."
  [applicator op form density?]
  `(do (view-histogram ~applicator
                       ~op
                       ~form
                       ~(str form)
                       ~density?)
       ~form))

(defn scope-histogram-frequency [form]
  (histogram-scope apply-row-op var-identity form false))

(defn scope-histogram-density [form]
  (histogram-scope apply-row-op var-identity form true))

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
