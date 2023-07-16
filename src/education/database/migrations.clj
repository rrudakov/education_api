(ns education.database.migrations
  (:require
   [education.system :as system]
   [integrant.core :as ig]
   [ragtime.next-jdbc]
   [ragtime.repl :as repl]
   [ragtime.strategy :as strategy]))

(defn- load-db-config
  [conn]
  {:datastore  (ragtime.next-jdbc/sql-database conn)
   :migrations (ragtime.next-jdbc/load-resources "migrations")
   :strategy   strategy/apply-new})

(defn migrate
  [{:keys [profile]}]
  (let [{:db/keys [connection] :as system}
        (ig/init (system/system-migration profile))]
    (repl/migrate (load-db-config connection))
    (ig/halt! system)))

(defn rollback
  [{:keys [profile]}]
  (let [{:db/keys [connection] :as system}
        (ig/init (system/system-migration profile))]
    (repl/rollback (load-db-config connection))
    (ig/halt! system)))
