(ns code-analysis.analyze
  (:require [kibit.check :refer [all-rules]]
            [kibit.core :as k]
            [clojure.walk :as walk]
            [clojure.tools.reader :as rdr]))

(defn simplify
  [rules expr]
  (->> expr
       (iterate (partial walk/prewalk #(k/simplify-one % rules)))
       (partition 2 1)
       (take-while #(apply not= %))))

(def read-codeq (comp rdr/read-string #(get-in % [:codeq/code :code/text])))

(defn analyze
  [codeq]
  (let [simplifications (simplify all-rules (read-codeq codeq))]
    {:codeq codeq
     :simplifications simplifications
     :kibit-quality (count simplifications)}))
