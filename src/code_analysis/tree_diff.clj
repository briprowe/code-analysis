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

(defmulti distance
  (fn []))
