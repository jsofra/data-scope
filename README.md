# data-scope

A Clojure library inspired by [Spyscope](https://github.com/dgrnbrg/spyscope) to provide tools for interactively visualizing data.

## Installation


#### Leiningen

Add `[jsofra/data-scope "0.1.0-SNAPSHOT"]` to your project.clj's `:dependencies`.

If you want data-scope to be automatically loaded and available in every project,
add the following to the `:user` profile in `~/.lein/profiles.clj`:

    :dependencies [[jsofra/data-scope "0.1.0-SNAPSHOT"]]
    :injections [(require 'data-scope.charts)
                 (require 'data-scope.graphs)]

#### Boot

After requiring the namespace, you must also run `(boot.core/load-data-readers!)`
to get the reader tags working. Using a `~/.boot/profile.boot` file:

```
(set-env! :dependencies #(conj % '[jsofra/data-scope "0.1.0-SNAPSHOT"]))

(require 'data-scope.charts)
(require 'data-scope.graphs)
(boot.core/load-data-readers!)
```

## Usage

Data-scope includes a number of reader tools for visualizing Clojure data.

Currently there are readers tags for visualizing data as both charts and graphs:

### Charts

Each of the chart tags may visualize:

* Numeric sequences
  * (range 10)
* Sequences of numeric sequences
  * [(range 10) (reverse (range 10))]
* Maps with numeric sequences as values
  * {:a (range 10) :b (reverse (range 10))}
  * except `#ds/p` which can visualize a map with numeric values as values
    * {:a 1 :b 2}

The tags are:

* `#ds/b` - *bar chart*
* `#ds/b-sum` - *summed bar chart*
* `#ds/b-max` - *maximum bar chart*
* `#ds/b-min` - *minimum bar chart*
* `#ds/l` - *line chart*
* `#ds/l-sum` - *summed line chart*
* `#ds/l-max` - *maximum line chart*
* `#ds/l-min` - *minimum line chart*
* `#ds/a` - *area chart*
* `#ds/sa` - *stacked area chart*
* `#ds/p` - *pie chart*
* `#ds/p-sum` - *summed pie chart*
* `#ds/p-max` - *maximum pie chart*
* `#ds/p-min` - *minimum pie chart*

#### Chart Examples

#### `#ds/b`

``` clojure
user> (let [data (range 4 14)]
        #ds/b data)
(4 5 6 7 8 9 10 11 12 13)
```
!["numeric sequence - bar chart"](/doc/bar_ns.png)

``` clojure
user> (let [data [(range 4 14) (reverse (range 2 12)) (range 8 18) (reverse (range 5 15))]]
        #ds/b data)
[(4 5 6 7 8 9 10 11 12 13) (11 10 9 8 7 6 5 4 3 2) (8 9 10 11 12 13 14 15 16 17) (14 13 12 11 10 9 8 7 6 5)]
```
!["sequence - bar chart"](/doc/bar_s_ns.png)

``` clojure
user> (let [data {:a (range 4 14) :b (reverse (range 2 12))}]
        #ds/b data)
{:a (4 5 6 7 8 9 10 11 12 13), :b (11 10 9 8 7 6 5 4 3 2)}
```
!["map - bar chart"](/doc/bar_map_ns.png)

#### `#ds/b-sum`

``` clojure
user> (let [data [(range 4 14) (reverse (range 2 12)) (range 8 18) (reverse (range 5 15))]]
        #ds/b-sum data)
[(4 5 6 7 8 9 10 11 12 13) (11 10 9 8 7 6 5 4 3 2) (8 9 10 11 12 13 14 15 16 17) (14 13 12 11 10 9 8 7 6 5)]
```
!["sequence - sum bar chart"](/doc/bar_s_ns_sum.png)

``` clojure
user> (let [data {:a (range 4 14) :b (reverse (range 2 12))}]
        #ds/b-sum data)
{:a (4 5 6 7 8 9 10 11 12 13), :b (11 10 9 8 7 6 5 4 3 2)}
```
!["sequence - sum bar chart"](/doc/bar_map_ns_sum.png)

#### `#ds/b-max`

``` clojure
user> (let [data [(range 4 14) (reverse (range 2 12)) (range 8 18) (reverse (range 5 15))]]
        #ds/b-max data)
[(4 5 6 7 8 9 10 11 12 13) (11 10 9 8 7 6 5 4 3 2) (8 9 10 11 12 13 14 15 16 17) (14 13 12 11 10 9 8 7 6 5)]
```
!["sequence - max bar chart"](/doc/bar_s_ns_max.png)

``` clojure
user> (let [data {:a (range 4 14) :b (reverse (range 2 12))}]
        #ds/b-max data)
{:a (4 5 6 7 8 9 10 11 12 13), :b (11 10 9 8 7 6 5 4 3 2)}
```
!["sequence - max bar chart"](/doc/bar_map_ns_max.png)

#### `#ds/b-min`

``` clojure
user> (let [data [(range 4 14) (reverse (range 2 12)) (range 8 18) (reverse (range 5 15))]]
        #ds/b-min data)
[(4 5 6 7 8 9 10 11 12 13) (11 10 9 8 7 6 5 4 3 2) (8 9 10 11 12 13 14 15 16 17) (14 13 12 11 10 9 8 7 6 5)]
```
!["sequence - min bar chart"](/doc/bar_s_ns_min.png)

``` clojure
user> (let [data {:a (range 4 14) :b (reverse (range 2 12))}]
        #ds/b-min data)
{:a (4 5 6 7 8 9 10 11 12 13), :b (11 10 9 8 7 6 5 4 3 2)}
```
!["sequence - min bar chart"](/doc/bar_map_ns_min.png)

#### `#ds/l`
``` clojure
user> (let [data (range 4 14)]
        #ds/l data)
(4 5 6 7 8 9 10 11 12 13)
```
!["numeric sequence - line chart"](/doc/line_ns.png)

``` clojure
user> (let [data [(range 4 14) (reverse (range 2 12)) (range 8 18) (reverse (range 5 15))]]
        #ds/l data)
[(4 5 6 7 8 9 10 11 12 13) (11 10 9 8 7 6 5 4 3 2) (8 9 10 11 12 13 14 15 16 17) (14 13 12 11 10 9 8 7 6 5)]
```
!["sequence - line chart"](/doc/line_s_ns.png)

``` clojure
user> (let [data {:a (range 4 14) :b (reverse (range 2 12))}]
        #ds/l data)
{:a (4 5 6 7 8 9 10 11 12 13), :b (11 10 9 8 7 6 5 4 3 2)}
```
!["map - line chart"](/doc/line_map_ns.png)

#### `#ds/l-sum`

``` clojure
user> (let [data [(range 4 14) (reverse (range 2 12)) (range 8 18) (reverse (range 5 15))]]
        #ds/l-sum data)
[(4 5 6 7 8 9 10 11 12 13) (11 10 9 8 7 6 5 4 3 2) (8 9 10 11 12 13 14 15 16 17) (14 13 12 11 10 9 8 7 6 5)]
```
!["sequence - sum line chart"](/doc/line_s_ns_sum.png)

#### `#ds/l-max`

``` clojure
user> (let [data [(range 4 14) (reverse (range 2 12)) (range 8 18) (reverse (range 5 15))]]
        #ds/l-max data)
[(4 5 6 7 8 9 10 11 12 13) (11 10 9 8 7 6 5 4 3 2) (8 9 10 11 12 13 14 15 16 17) (14 13 12 11 10 9 8 7 6 5)]
```
!["sequence - max line chart"](/doc/line_s_ns_max.png)

#### `#ds/l-min`

``` clojure
user> (let [data [(range 4 14) (reverse (range 2 12)) (range 8 18) (reverse (range 5 15))]]
        #ds/l-min data)
[(4 5 6 7 8 9 10 11 12 13) (11 10 9 8 7 6 5 4 3 2) (8 9 10 11 12 13 14 15 16 17) (14 13 12 11 10 9 8 7 6 5)]
```
!["sequence - min line chart"](/doc/line_s_ns_min.png)

#### `#ds/a`

``` clojure
user> (let [data (range 4 14)]
        #ds/a data)
(4 5 6 7 8 9 10 11 12 13)
```
!["numeric sequence - area chart"](/doc/area_ns.png)

``` clojure
user> (let [data [(range 4 14) (reverse (range 2 12)) (range 8 18) (reverse (range 5 15))]]
        #ds/a data)
[(4 5 6 7 8 9 10 11 12 13) (11 10 9 8 7 6 5 4 3 2) (8 9 10 11 12 13 14 15 16 17) (14 13 12 11 10 9 8 7 6 5)]
```
!["sequence - area chart"](/doc/area_s_ns.png)

``` clojure
user> (let [data {:a (range 4 14) :b (reverse (range 2 12))}]
        #ds/a data)
{:a (4 5 6 7 8 9 10 11 12 13), :b (11 10 9 8 7 6 5 4 3 2)}
```
!["map - area chart"](/doc/area_map_ns.png)

#### `#ds/sa`

``` clojure
user> (let [data (range 4 14)]
        #ds/sa data)
(4 5 6 7 8 9 10 11 12 13)
```
!["numeric sequence - stacked area chart"](/doc/stacked_area_ns.png)

``` clojure
user> (let [data [(range 4 14) (reverse (range 2 12)) (range 8 18) (reverse (range 5 15))]]
        #ds/sa data)
[(4 5 6 7 8 9 10 11 12 13) (11 10 9 8 7 6 5 4 3 2) (8 9 10 11 12 13 14 15 16 17) (14 13 12 11 10 9 8 7 6 5)]
```
!["sequence - stacked area chart"](/doc/stacked_area_s_ns.png)

``` clojure
user> (let [data {:a (range 4 14) :b (reverse (range 2 12))}]
        #ds/sa data)
{:a (4 5 6 7 8 9 10 11 12 13), :b (11 10 9 8 7 6 5 4 3 2)}
```
!["map - stacked area chart"](/doc/stacked_area_map_ns.png)

#### `#ds/p`

``` clojure
user> (let [data (range 1 4)]
        #ds/p data)
(1 2 3)
```
!["numeric sequence - pie chart"](/doc/pie_ns.png)

``` clojure
user> (let [data [(range 1 4) (range 1 4)]]
        #ds/p data)
[(1 2 3) (1 2 3)]
```
!["sequence - pie chart"](/doc/pie_s_ns.png)

``` clojure
user> (let [data {:a 1 :b 2 :c 4}]
        #ds/p data)
{:a 1, :b 2, :c 4}
```
!["map - pie chart"](/doc/pie_map_ns.png)

#### `#ds/p-sum`

``` clojure
user> (let [data {:a (range 4 14) :b (reverse (range 2 12))}]
        #ds/p-sum data)
{:a (4 5 6 7 8 9 10 11 12 13), :b (11 10 9 8 7 6 5 4 3 2)}
```
!["sequence - summed pie chart"](/doc/pie_map_ns_sum.png)

#### `#ds/p-max`

``` clojure
user> (let [data {:a (range 4 14) :b (reverse (range 2 12))}]
        #ds/p-max data)
{:a (4 5 6 7 8 9 10 11 12 13), :b (11 10 9 8 7 6 5 4 3 2)}
```
!["sequence - max pie chart"](/doc/pie_map_ns_max.png)

#### `#ds/p-min`

``` clojure
user> (let [data {:a (range 4 14) :b (reverse (range 2 12))}]
        #ds/p-min data)
{:a (4 5 6 7 8 9 10 11 12 13), :b (11 10 9 8 7 6 5 4 3 2)}
```
!["sequence - min pie chart"](/doc/pie_map_ns_min.png)

### Graphs

The graph tags are:

* `#ds/g` - *graph viz*
* `#ds/t` - *tree viz*
* `#ds/trie` - *trie viz*
* `#ds/d` - *dot graph viz*

#### Graph Examples

#### `#ds/g`

``` clojure
user> (let [data {:a [:b :c]
                  :b [:c]
                  :c [:a]}]
        #ds/g data)
{:a [:b :c], :b [:c], :c [:a]}
```
!["graph example"](/doc/graph.png)

#### `#ds/t`

``` clojure
user> (let [data [[1 [2 3]] [4 [5]]]]
        #ds/t data)
[[1 [2 3]] [4 [5]]]
```
!["tree example"](/doc/tree.png)

#### `#ds/trie`

``` clojure
user> (let [data '([1 2] ([3 4] ([5 6 7])))]
        #ds/trie data)
([1 2] ([3 4] ([5 6 7])))
```
!["trie example"](/doc/trie.png)


## License

Copyright © 2016 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
