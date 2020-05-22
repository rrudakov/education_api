(ns education.http.component
  (:require [buddy.auth.middleware :refer [wrap-authentication wrap-authorization]]
            [com.stuartsierra.component :as component]
            [education.config :as config]
            [education.http.routes :refer [api-routes]]
            [org.httpkit.server :as server]
            [ring.middleware.cors :refer [wrap-cors]]
            [ring.middleware.defaults :refer [api-defaults wrap-defaults]]))

(defrecord WebServer [srv config db]
  component/Lifecycle

  (start [this]
    (if srv
      this
      (let [port (config/application-port config)
            auth-backend (config/auth-backend config)]
        (println (str ";; Running web server at http://127.0.0.1:" port "/"))
        (assoc this :srv
               (server/run-server
                (-> db
                    (api-routes config)
                    (wrap-cors :access-control-allow-origin [#".*"]
                               :access-control-allow-headers ["Origin" "Accept" "Content-Type" "Authorization" "X-Requested-With" "Cache-Control"]
                               :access-control-allow-methods [:get :post :patch :put :delete])
                    (wrap-authorization auth-backend)
                    (wrap-authentication auth-backend)
                    (muuntaja.middleware/wrap-format)
                    (wrap-defaults api-defaults))
                {:port port})))))

  (stop [this]
    (if srv
      (do
        (println ";; Stopping web server")
        (srv :timeout 100)
        (reduce #(assoc %1 %2 nil) this [:srv :db]))
      this)))

(defn new-webserver
  [config]
  (component/using
   (map->WebServer {:config config}) [:db]))
