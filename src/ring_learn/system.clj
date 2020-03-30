(ns ring-learn.system
  (:require [com.stuartsierra.component :as component]
            [ring-learn.database.component :refer [new-database]]
            [ring-learn.http.component :refer [new-webserver]]
            [ring-learn.config :as config]))

(defn api-system
  [profile]
  (let [config (config/config profile)]
    (component/system-map
     :db (new-database config)
     :http-server (new-webserver config))))
