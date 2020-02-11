(ns ring-learn.system
  (:require [com.stuartsierra.component :as component]
            [ring-learn.database.component :refer [new-database]]
            [ring-learn.http.component :refer [new-webserver]]))

(defn api-system
  [config]
  (component/system-map
   :db (new-database)
   :http-server (new-webserver)))
