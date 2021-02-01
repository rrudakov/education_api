(ns education.system
  (:require [buddy.auth.middleware :refer [wrap-authentication wrap-authorization]]
            [compojure.api.api :refer [api-defaults]]
            [education.config :as config]
            [education.http.routes :refer [api-routes]]
            [integrant.core :as ig]
            [muuntaja.middleware :refer [wrap-format]]
            [next.jdbc.connection :as connection]
            [ring.middleware.cors :refer [wrap-cors]]
            [ring.middleware.defaults :refer [wrap-defaults]]
            [org.httpkit.server :as server])
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
  (config/config profile))

(defmethod ig/init-key :database.sql/connection
  [_ {:keys [config]}]
  (let [db-spec (config/db-spec config)]
    (connection/->pool ComboPooledDataSource db-spec)))

(defmethod ig/halt-key! :database.sql/connection
  [_ datasource]
  (.close datasource))

(defmethod ig/init-key :handler/run-app
  [_ {:keys [db config]}]
  (let [auth-backend (config/auth-backend config)]
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
  (server/run-server handler {:port (config/application-port config)}))

(defmethod ig/halt-key! :adapter/http-kit
  [_ srv]
  (srv :timeout 100))
