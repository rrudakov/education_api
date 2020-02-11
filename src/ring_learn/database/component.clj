(ns ring-learn.database.component
  (:require [com.stuartsierra.component :as component]
            [ragtime.jdbc :as jdbc]
            [ragtime.repl :as repl])
  (:import com.mchange.v2.c3p0.ComboPooledDataSource))

(def db-spec
  {:dbtype "postgresql"
   :host "127.0.0.1"
   :dbname "clojuredb"
   :user "clojure"
   :password "123456"})

(defn pool
  [spec]
  (let [cpds (doto (ComboPooledDataSource.)
               (.setDriverClass "org.postgresql.Driver")
               (.setJdbcUrl (str "jdbc:" (:dbtype spec) "://" (:host spec) "/" (:dbname spec)))
               (.setUser (:user spec))
               (.setPassword (:password spec))
               (.setMaxIdleTimeExcessConnections (* 30 60))
               (.setMaxIdleTime (* 3 60 60)))]
    {:datasource cpds}))

;; Migrations
(defn load-db-config
  []
  {:datastore (jdbc/sql-database db-spec)
   :migrations (jdbc/load-resources "migrations")})

(defn migrate
  []
  (repl/migrate (load-db-config)))

(defn rollback
  []
  (repl/rollback (load-db-config)))

;; Global database connection pool
(defrecord Database [connection]
  component/Lifecycle

  (start [component]
    (println ";; Starting database")
    (let [conn (pool db-spec)]
      (assoc component :connection conn)))

  (stop [component]
    (println ";; Stopping database")
    (.close (:datasource connection))
    (assoc component :connection nil)))

(defn new-database
  []
  (map->Database {}))
