(ns code-analysis.report
  (:require [datomic.api :as d :refer [q]]
            [code-analysis.query :as q]))

(defn codeq->node-object
  [codeq]
  (-> (get-in codeq [:codeq/file :node/_object])
      first))

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
  (let [commit (first (sort-by :commit/authoredAt (comp - compare) (q/codeq->commit codeq)))
        location (->location codeq)]
    {;; :sha (get-in codeq [:codeq/file :git/sha])
     :url (->url (:git/sha commit) location)
     :author (get-in commit [:commit/author :email/address])
     :kibit-quality (:kibit-quality analysis)}))

(defn print-report
  [analysis]
  (->> (map analysis->report analysis)
       clojure.pprint/print-table))
