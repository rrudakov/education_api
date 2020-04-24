(ns ring-learn.http.component
  (:require [buddy.auth.middleware :refer [wrap-authentication wrap-authorization]]
            [com.stuartsierra.component :as component]
            [org.httpkit.server :as server]
            [ring-learn.config :as config]
            [ring-learn.http.routes :refer [api-routes]]
            [ring.middleware.defaults :refer [api-defaults wrap-defaults]]))

(defrecord WebServer [srv config db]
  component/Lifecycle

  (start [this]
    (let [port (config/application-port config)]
      (println (str ";; Running web server at http://127.0.0.1:" port "/"))
      (assoc this :srv
             (server/run-server
              (-> db
                  (api-routes config)
                  (wrap-authorization (config/auth-backend config))
                  (wrap-authentication (config/auth-backend config))
                  (wrap-defaults api-defaults))
              {:port port}))))

  (stop [this]
    (println ";; Stopping web server")
    (srv :timeout 100)
    (reduce #(assoc %1 %2 nil) this [:srv :db])))

(defn new-webserver
  [config]
  (component/using
   (map->WebServer {:config config}) [:db]))
