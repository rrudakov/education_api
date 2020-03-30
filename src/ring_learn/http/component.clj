(ns ring-learn.http.component
  (:require [com.stuartsierra.component :as component]
            [org.httpkit.server :as server]
            [ring-learn.http.routes :refer [api-routes]]
            [ring.middleware.defaults :refer [api-defaults wrap-defaults]]
            [ring-learn.config :as config]))

(defrecord WebServer [srv port db]
  component/Lifecycle

  (start [this]
    (println (str ";; Running web server at http://127.0.0.1:" port "/"))
    (assoc this :srv
           (server/run-server (wrap-defaults (api-routes db) api-defaults)
                              {:port port})))

  (stop [this]
    (println ";; Stopping web server")
    (srv :timeout 100)
    (reduce #(assoc %1 %2 nil) this [:srv :db])))

(defn new-webserver
  [config]
  (component/using
   (map->WebServer {:port (config/application-port config)}) [:db]))
