(ns code-analysis.core
  (:require [datomic.api :as d :refer [q]]
            [clojure.set :as set]))

(def connection (d/connect "datomic:free://localhost:4334/message-api2"))
(def db (d/db connection))

(def sha "8d15e962510e5812618893bb80dd457f5d190525")

(comment

  )
