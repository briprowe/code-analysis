(ns code-analysis.schema
  (:require [datomic.api :as d]))

(def schema
  [{:db/id #db/id[:db.part/db]
    :db/ident :analysis/codeq
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one
    :db/doc "Associate this analysis with a codeq."
    :db.install/_attribute :db.part/db}

   {:db/id #db/id[:db.part/db]
    :db/ident :analysis/simplifications
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc
    (str "A edn representation of the simplifications"
         " that Kibit found for :analysis/codeq.")
    :db.install/_attribute :db.part/db}

   {:db/id #db/id[:db.part/db]
    :db/ident :analysis/kibit-quality
    :db/valueType :db.type/long
    :db/cardinality :db.cardinality/one
    :db/doc
    (str "The number of \"steps\" that Kibit iterated "
         "through to fully simplify :analysis/codeq.")
    :db.install/_attribute :db.part/db}]
  )

(defn install-schema
  [conn]
  (or (-> conn d/db (d/entid :analysis/codeq))
      @(d/transact conn schema)))
