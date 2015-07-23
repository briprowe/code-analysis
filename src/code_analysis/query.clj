(ns code-analysis.query
  (:require [datomic.api :as d :refer [q]]))

(def rules
 '[[(node-files ?n ?f) [?n :node/object ?f] [?f :git/type :blob]]
   [(node-files ?n ?f) [?n :node/object ?t] [?t :git/type :tree]
                       [?t :tree/nodes ?n2] (node-files ?n2 ?f)]
   [(object-nodes ?o ?n) [?n :node/object ?o]]
   [(object-nodes ?o ?n) [?n2 :node/object ?o] [?t :tree/nodes ?n2] (object-nodes ?t ?n)]
   [(commit-files ?c ?f) [?c :commit/tree ?root] (node-files ?root ?f)]
   [(commit-codeqs ?c ?cq) (commit-files ?c ?f) [?cq :codeq/file ?f]]
   [(file-commits ?f ?c) (object-nodes ?f ?n) [?c :commit/tree ?n]]
   [(codeq-commits ?cq ?c) [?cq :codeq/file ?f] (file-commits ?f ?c)]
   [(sha-codeqs ?sha ?cq) [?commit :git/sha ?sha] [?commit :git/type :commit]
                          (commit-codeqs ?commit ?cq)]

   [(sha-parent-codeqs ?sha ?cq)
    [?commit :git/sha ?sha]
    [?commit :git/type :commit]
    [?commit :commit/parents ?parent-commit]
    (commit-codeqs ?parent-commit ?cq)]])

(defn sha-lookup
  [lookup-rule]
  (let [query (conj '[:find ?result
                      :in $ % ?commit-sha
                      :where]
                    lookup-rule)]
    (fn [db sha] (q query db rules sha))))

(def commit-codeq (sha-lookup '(sha-codeqs ?commit-sha ?result)))
(def parent-codeq (sha-lookup '(sha-parent-codeqs ?commit-sha ?result)))

(def codeq-sha #(get-in % [:codeq/code :code/sha]))

(defn query-result->codeq
  [db]
  (comp (map first)
        (map (partial d/entity db))))

(defn query-result->sha
  [db]
  (comp (query-result->codeq db)
        (map codeq-sha)))

(defn commit->codeq
  [db commit-sha]
  (let [parent-shas (into #{} (query-result->sha db)
                          (parent-codeq db commit-sha))]
    (into [] (comp
              (query-result->codeq db)
              (remove (comp parent-shas codeq-sha)))
          (commit-codeq db commit-sha))))

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

(defn analysis->transaction
  [{:keys [codeq simplifications kibit-quality] :as analysis}]
  (let [analysis-id (d/tempid :db.part/user)]
    [[:db/add analysis-id :analysis/codeq (:db/id codeq)]
     [:db/add analysis-id :analysis/simplifications (pr-str simplifications)]
     [:db/add analysis-id :analysis/kibit-quality kibit-quality]]))

(defn store-analysis
  [conn analysis]
  (->> analysis
       (into [] (mapcat analysis->transaction))
       (d/transact conn)))

(comment
  (do
    (require 'code-analysis.core)
    (require 'code-analysis.analyze)
    (require 'code-analysis.report)

    @(->> (commit->codeq (d/db code-analysis.core/connection)
                         "982109e01bf00454282c4ee6a805610738be04a1")
          (map code-analysis.analyze/analyze)
          (store-analysis code-analysis.core/connection)))

  (q '[:find ?analysis
       :where
       [?analysis :analysis/codeq _]]
     (d/db code-analysis.core/connection))

  )
