(defproject code-analysis "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [com.datomic/datomic-free "0.9.5198"]
                 [jonase/kibit "0.1.2"]
                 [org.clojure/tools.reader "0.10.0-alpha1"]
                 [clj-diff "1.0.0-SNAPSHOT" :exclusions [org.clojure]]])
