(ns user
  (:require
   [education.database.gymnastics :as database.gymnastics]
   [education.database.users :as database.users]
   [education.system :as system]
   [integrant.core :as ig]))

(defonce system (atom nil))

(defn- db
  []
  (:db/connection @system))

(defn- start-dev []
  (reset! system (ig/init system/system-config-dev))
  :started)

(defn- stop []
  (swap! system ig/halt!))

(comment
  (start-dev)
  (stop)

  (:system/config @system)

  (database.users/get-all-users (db))
  (database.gymnastics/get-all-gymnastics (db) 1)
  (database.gymnastics/get-gymnastic-by-id (db) 5)
  )
