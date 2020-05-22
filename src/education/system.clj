(ns education.system
  (:require [com.stuartsierra.component :as component]
            [education.config :as config]
            [education.database.component :refer [new-database]]
            [education.http.component :refer [new-webserver]]))

(defn api-system
  [profile]
  (let [config (config/config profile)]
    (component/system-map
     :db (new-database config)
     :http-server (new-webserver config))))
