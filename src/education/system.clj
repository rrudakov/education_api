(ns education.system
  (:require
   [buddy.auth.middleware :refer [wrap-authentication wrap-authorization]]
   [com.brunobonacci.mulog :as u]
   [compojure.api.api :refer [api-defaults]]
   [education.config :as config]
   [education.http.routes :as r :refer [api-routes]]
   [hikari-cp.core :refer [close-datasource make-datasource]]
   [integrant.core :as ig]
   [muuntaja.middleware :refer [wrap-format]]
   [next.jdbc.connection :as connection]
   [org.httpkit.server :as server]
   [reitit.ring :as ring]
   [ring.adapter.jetty :refer [run-jetty]]
   [ring.middleware.cors :refer [wrap-cors]]
   [ring.middleware.defaults :refer [wrap-defaults]]
   [taoensso.timbre :refer [info]])
  (:import
   com.mchange.v2.c3p0.ComboPooledDataSource))

(defn system-config
  [profile]
  {:system/config           {:profile profile}
   :adapter/http-kit        {:handler (ig/ref :handler/run-app)
                             :config  (ig/ref :system/config)}
   :handler/run-app         {:config (ig/ref :system/config)
                             :db     (ig/ref :database.sql/connection)}
   :database.sql/connection {:config (ig/ref :system/config)}})

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

;; Reitit

(def system-config-dev
  {:system/config    {:profile :dev}
   :db/connection    {:config (ig/ref :system/config)}
   :http/router-opts {:config (ig/ref :system/config)
                      :db     (ig/ref :db/connection)}
   :http/router-dev  {:router-opts (ig/ref :http/router-opts)}
   :http/handler     {:router (ig/ref :http/router-dev)}
   :http/adapter     {:handler (ig/ref :http/handler)
                      :config  (ig/ref :system/config)}})

(def system-config-prod
  {:system/config    {:profile :dev}
   :db/connection    {:config (ig/ref :system/config)}
   :http/router-opts {:config (ig/ref :system/config)
                      :db     (ig/ref :db/connection)}
   :http/router-prod {:router-opts (ig/ref :http/router-opts)}
   :http/handler     {:router (ig/ref :http/router-prod)}
   :http/adapter     {:handler (ig/ref :http/handler)
                      :config  (ig/ref :system/config)}})

(defmethod ig/init-key :system/config
  [_ {:keys [profile]}]
  (u/log ::read-configuration :profile profile)
  (config/config profile))

(defmethod ig/init-key :db/connection
  [_ {:keys [config]}]
  (let [db-spec (config/db-spec config)]
    (u/log ::make-datasource :db-spec db-spec)
    (make-datasource db-spec)))

(defmethod ig/halt-key! :db/connection
  [_ datasource]
  (u/log ::close-datasource :datasource datasource)
  (close-datasource datasource))

(defmethod ig/init-key :http/router-opts
  [_ {:keys [config db]}]
  (r/router-options db config))

(defmethod ig/init-key :http/router-dev
  [_ {:keys [router-opts]}]
  (ring/router (r/routes) router-opts))

(defmethod ig/init-key :http/router-prod
  [_ {:keys [router-opts]}]
  (ring/router (r/prod-routes) router-opts))

(defmethod ig/init-key :http/handler
  [_ {:keys [router]}]
  (ring/ring-handler
   router
   (ring/routes
    (ring/redirect-trailing-slash-handler)
    (ring/create-default-handler
     {:not-found          (constantly {:status 404 :body "Not found"})
      :method-not-allowed (constantly {:status 405 :body "Method not allowed"})
      :not-acceptable     (constantly {:status 406 :body "Not acceptable"})}))))

(defmethod ig/init-key :http/adapter
  [_ {:keys [handler config]}]
  (let [port (config/application-port config)]
    (u/log ::start-jetty :port port)
    (run-jetty handler {:port port :join? false})))

(defmethod ig/halt-key! :http/adapter
  [_ srv]
  (.stop srv))
