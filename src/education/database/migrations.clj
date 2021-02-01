(ns education.database.migrations
  (:require [education.config :as config]
            [ragtime.jdbc :as jdbc]
            [ragtime.repl :as repl]))

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
