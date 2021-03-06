(ns education.system
  (:require [buddy.auth.middleware :refer [wrap-authentication wrap-authorization]]
            [compojure.api.api :refer [api-defaults]]
            [education.config :as config]
            [education.http.routes :refer [api-routes]]
            [integrant.core :as ig]
            [muuntaja.middleware :refer [wrap-format]]
            [next.jdbc.connection :as connection]
            [org.httpkit.server :as server]
            [ring.middleware.cors :refer [wrap-cors]]
            [ring.middleware.defaults :refer [wrap-defaults]]
            [taoensso.timbre :refer [info]])
  (:import com.mchange.v2.c3p0.ComboPooledDataSource))

(defn system-config
  [profile]
  {:system/config
   {:profile profile}
   :adapter/http-kit
   {:handler (ig/ref :handler/run-app)
    :config  (ig/ref :system/config)}
   :handler/run-app
   {:config (ig/ref :system/config)
    :db     (ig/ref :database.sql/connection)}
   :database.sql/connection
   {:config (ig/ref :system/config)}})

(defmethod ig/init-key :system/config
  [_ {:keys [profile]}]
  (info "Read configuration")
  (config/config profile))

(defmethod ig/init-key :database.sql/connection
  [_ {:keys [config]}]
  (let [db-spec (config/db-spec config)]
    (info "Initialize database connection pool")
    (connection/->pool ComboPooledDataSource db-spec)))

(defmethod ig/halt-key! :database.sql/connection
  [_ datasource]
  (info "Close database connections")
  (.close datasource))

(defmethod ig/init-key :handler/run-app
  [_ {:keys [db config]}]
  (let [auth-backend (config/auth-backend config)]
    (info "Initialize handler")
    (-> db
        (api-routes config)
        (wrap-cors :access-control-allow-origin [#".*"]
                   :access-control-allow-headers ["Origin" "Accept" "Content-Type" "Authorization" "X-Requested-With" "Cache-Control"]
                   :access-control-allow-methods [:get :post :patch :put :delete])
        (wrap-authorization auth-backend)
        (wrap-authentication auth-backend)
        (wrap-format)
        (wrap-defaults api-defaults))))

(defmethod ig/init-key :adapter/http-kit
  [_ {:keys [handler config]}]
  (let [port (config/application-port config)]
    (info "Start server on port " port)
    (server/run-server handler {:port (config/application-port config)})))

(defmethod ig/halt-key! :adapter/http-kit
  [_ srv]
  (info "Stop server")
  (srv :timeout 100))
