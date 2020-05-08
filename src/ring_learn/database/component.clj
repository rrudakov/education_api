(ns ring-learn.database.component
  (:require [com.stuartsierra.component :as component]
            [next.jdbc :as j]
            [next.jdbc.connection :as connection]
            [ragtime.jdbc :as jdbc]
            [ragtime.repl :as repl]
            [ring-learn.config :as config])
  (:import com.mchange.v2.c3p0.ComboPooledDataSource))

;; Migrations
(defn load-db-config
  [profile]
  (let [db-uri (:url (config/db-config (config/config profile)))]
    {:datastore (jdbc/sql-database {:connection-uri db-uri})
     :migrations (jdbc/load-resources "migrations")}))

(defn migrate
  [profile]
  (repl/migrate (load-db-config (keyword profile))))

(defn rollback
  [profile]
  (repl/rollback (load-db-config (keyword profile))))

;; Global database connection pool
(defrecord Database [db-spec datasource]
  component/Lifecycle

  (start [this]
    (println ";; Starting database")
    (if datasource
      this
      (assoc this :datasource
             (connection/->pool ComboPooledDataSource db-spec))))

  (stop [this]
    (println ";; Stopping database")
    (if datasource
      (do
        (.close datasource)
        (assoc this :datasource nil))
      this)))

(defn new-database
  [config]
  (map->Database {:db-spec (config/db-spec config)}))
