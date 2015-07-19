(ns code-analysis.core
  (:require [datomic.api :as d :refer [q]]
            [kibit.check :refer [all-rules]]
            [kibit.core :as k]
            [clojure.walk :as walk]
            [clojure.tools.reader :as rdr]))

(def connection (d/connect "datomic:free://localhost:4334/message-api"))
(def db (d/db connection))

(def rules
 '[[(node-files ?n ?f) [?n :node/object ?f] [?f :git/type :blob]]
   [(node-files ?n ?f) [?n :node/object ?t] [?t :git/type :tree]
                       [?t :tree/nodes ?n2] (node-files ?n2 ?f)]
   [(object-nodes ?o ?n) [?n :node/object ?o]]
   [(object-nodes ?o ?n) [?n2 :node/object ?o] [?t :tree/nodes ?n2] (object-nodes ?t ?n)]
   [(commit-files ?c ?f) [?c :commit/tree ?root] (node-files ?root ?f)]
   [(commit-codeqs ?c ?cq) (commit-files ?c ?f) [?cq :codeq/file ?f]]
   [(file-commits ?f ?c) (object-nodes ?f ?n) [?c :commit/tree ?n]]
   [(codeq-commits ?cq ?c) [?cq :codeq/file ?f] (file-commits ?f ?c)]])

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

(defn codeq->node-object
  [codeq]
  (-> (get-in codeq [:codeq/file :node/_object])
      first))

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

(defn codeq->paths
  [codeq]
  (->> (:node/paths (codeq->node-object codeq))
       (map :file/name)))

(defn ->location
  [codeq]
  (merge {:path (.replaceFirst (first (codeq->paths codeq)) "message-api/" "")}
         (->> (re-seq #"\d+" (:codeq/loc codeq))
              (map #(Long/parseLong %))
              (zipmap [:start-line :start-col :end-line :end-col]))))

(defn ->url
  [sha {:keys [path start-line end-line]}]
  (format "https://github.com/rentpath/message-api/blob/%s/%s#L%d-%d"
          sha path start-line end-line))

(defn analysis->report
  [{:keys [codeq] :as analysis}]
  (let [commit (first (sort-by :commit/authoredAt (comp - compare) (codeq->commit codeq)))
        location (->location codeq)]
    {;; :sha (get-in codeq [:codeq/file :git/sha])
     :url (->url (:git/sha commit) location)
     :author (get-in commit [:commit/author :email/address])
     :kibit-quality (:kibit-quality analysis)}))

(defn print-report
  [analysis]
  (->> (map analysis->report analysis)
       clojure.pprint/print-table))

(comment
  (require '[clojure.java.io :as io])
  
  (with-open [output-writer (io/writer "analysis.txt")]
    (binding [*out* output-writer]
      (->> (codeqs db) (pmap analyze) print-report)))

  )
