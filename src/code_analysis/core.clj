(ns code-analysis.core
  (:require [datomic.api :as d :refer [q]]
            [clojure.set :as set]))

(def connection (d/connect "datomic:free://localhost:4334/message-api"))
(def db (d/db connection))

(def sha "8d15e962510e5812618893bb80dd457f5d190525")

;; (defn codeqs
;;   [db]
;;   (->> (q '[:find (sample 10 ?codeq) .
;;             :where
;;             [?codeq :codeq/code]]
;;           db)
;;        ;; (map first)
;;        (map (partial d/entity db))
;;        ))

(defn codeqs
  [db]
  (->> (q '[:find ?commit ?codeq
            :in $ %
            :where
            [?commit :git/type :commit]
            (commit-codeqs ?commit ?codeq)]
          db rules)))



(defn codeq->commit
  [codeq]
  (q '[:find [(pull ?commit [:commit/message
                             :git/sha
                             {:commit/author [:email/address]}
                             :commit/authoredAt])]
       :in $ % ?codeq
       :where
       (codeq-commits ?codeq ?commit)]
     (d/entity-db codeq) rules
     (:db/id codeq)))

(comment
  (require '[clojure.java.io :as io])
  
  (with-open [output-writer (io/writer "analysis.txt")]
    (binding [*out* output-writer]
      (->> (codeqs db) (pmap analyze) print-report)))

  )
