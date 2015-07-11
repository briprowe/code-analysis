(ns code-analysis.core
  (:require [datomic.api :as d :refer [q]]
            [kibit.check :as k]
            [clojure.tools.reader.edn :as edn]))

(def connection (d/connect "datomic:free://localhost:4334/message-api"))
(def db (d/db connection))

(def sha "8d15e962510e5812618893bb80dd457f5d190525")

(def commit (d/touch (d/entity db 17592186066529)))


(defmulti find-files :git/type)

(defmethod find-files :commit
  [commit]
  (flatten (find-files (get-in commit [:commit/tree :node/object]))))

(defmethod find-files :tree
  [tree-node]
  (map (comp find-files :node/object) (:tree/nodes tree-node)))

(defmethod find-files :blob
  [blob-object]
  blob-object)


(def analysis
  (k/check-expr
   (edn/read-string
    (ffirst (q '[:find ?code-text
                 :where
                 [?file :git/type :blob]
                 [?codeq :codeq/file ?file]
                 [?codeq :codeq/code ?code]
                 [?code :code/text ?code-text]]
               db)))))

