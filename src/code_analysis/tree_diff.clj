(ns code-analysis.tree-diff
  (:refer-clojure :exclude [compare]))

(defn classify
  [x]
  (cond
    (list? x)    :list
    (vector? x)  :vector
    (map? x)     :map
    (set? x)     :set
    (number? x)  :number
    (string? x)  :string
    (char? x)    :char
    (symbol? x)  :symbol
    (keyword? x) :keyword))

(def comparison-hierarchy
  (-> (make-hierarchy)
      (derive :number  ::atomic)
      (derive :string  ::atomic)
      (derive :char    ::atomic)
      (derive :symbol  ::atomic)
      (derive :keyword ::atomic)
      (derive :list    ::sequential)
      (derive :vector  ::sequential)
      (derive :map     ::indexed)
      (derive :set     ::indexed)
      (derive :vector  ::indexed)))

(defmulti compare
  (fn [x y] [(classify x) (classify y)])
  :hierarchy #'comparison-hierarchy)

(defmethod compare [::atomic ::atomic]
  [x y]
  (if (= x y) 0 1))

(defmethod compare [::sequential ::sequential]
  [x y]
  (reduce + (map compare x y)))

(defmethod compare :default
  [x y]
  (if (= x y) 0 1))

(def edit-costs
  {:change-label 1
   :delete 1
   :insert 1})

(defn ->trace
  [[edit-name :as edit]]
  {:cost (get edit-costs edit-name)
   :trace [edit]})

(defn trace+
  [trace1 trace2]
  (-> trace1
      (update :cost + (:cost trace2))
      (update :trace into (:trace trace2))))

(defmulti ->cost-table
  (fn [t1 t2] [(classify t1) (classify t2)])
  :hierarchy #'comparison-hierarchy)

(defn previous
  [table [row col]]
  (cond 
    (zero? row) (get table [row (dec col)])
    (zero? col) (get table [(dec row) col])
    :else (get table [(dec row) (dec col)])))

(def initial-inserts (map-indexed (fn [i node] [[(inc i) 0] (->trace [:delete node])])))
(def initial-deletes (map-indexed (fn [j node] [[0 (inc j)] (->trace [:insert node])])))

(defn insert-trace
  [table index trace]
  (assoc table index (trace+ (previous table index) trace)))

(defmethod ->cost-table [::sequential ::sequential]
  [t1 t2]
  (reduce-kv insert-trace
             {[0 0] (->trace [:change-label t1 t2])}
             (-> {}
                 (into initial-inserts t1)
                 (into initial-deletes t2))))

(defmethod ->cost-table [::atomic ::atomic]
  [t1 t2]
  (->cost-table [t1] [t2]))

(defmulti tree-diff*
  (fn [node1 node2 cost]
    [(classify node1) (classify node2)])
  :hierarchy #'comparison-hierarchy)

;; (defmulti tree-diff* [::atomic ::atomic]
;;   [node1 node2 cost]
;;   )

(defn tree-diff
  ([t1 t2] (tree-diff t1 t2 (->cost-table t1 t2)))
  ([t1 t2 cost-table]
   cost-table))

