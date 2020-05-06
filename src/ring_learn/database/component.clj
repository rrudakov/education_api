(ns ring-learn.database.component
  (:require [com.stuartsierra.component :as component]
            [ragtime.jdbc :as jdbc]
            [ragtime.repl :as repl]
            [ring-learn.config :as config])
  (:import com.mchange.v2.c3p0.ComboPooledDataSource))

(defn pool
  [spec]
  (let [cpds (doto (ComboPooledDataSource.)
               (.setDriverClass "org.postgresql.Driver")
               (.setJdbcUrl (:uri spec))
               (.setMaxIdleTimeExcessConnections (* 30 60))
               (.setMaxIdleTime (* 3 60 60)))]
    {:datasource cpds}))

;; Migrations
(defn load-db-config
  [profile]
  (let [db-uri (:uri (config/db-spec (config/config profile)))]
    {:datastore (jdbc/sql-database {:connection-uri db-uri})
     :migrations (jdbc/load-resources "migrations")}))

(defn migrate
  [profile]
  (repl/migrate (load-db-config profile)))

(defn rollback
  [profile]
  (repl/rollback (load-db-config profile)))

;; Global database connection pool
(defrecord Database [connection db-spec]
  component/Lifecycle

  (start [this]
    (println ";; Starting database")
    (let [conn (pool db-spec)]
      (assoc this :connection conn)))

  (stop [this]
    (println ";; Stopping database")
    (.close (:datasource connection))
    (assoc this :connection nil)))

(defn new-database
  [config]
  (->Database {} (config/db-spec config)))
