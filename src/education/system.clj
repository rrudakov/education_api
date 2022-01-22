(ns education.system
  (:require
   [com.brunobonacci.mulog :as u]
   [education.config :as config]
   [education.http.routes :as r]
   [hikari-cp.core :refer [close-datasource make-datasource]]
   [integrant.core :as ig]
   [next.jdbc.result-set :as rs]
   [org.httpkit.server :as server]
   [reitit.ring :as ring])
  (:import
   java.sql.Array))

(def system-config-dev
  {:system/config    {:profile :dev}
   :ulog/publisher   {:config (ig/ref :system/config)}
   :db/connection    {:config (ig/ref :system/config)}
   :http/router-opts {:config (ig/ref :system/config)
                      :db     (ig/ref :db/connection)}
   :http/router-dev  {:router-opts (ig/ref :http/router-opts)}
   :http/handler     {:router (ig/ref :http/router-dev)}
   :http/adapter     {:handler (ig/ref :http/handler)
                      :config  (ig/ref :system/config)}})

(def system-config-prod
  {:system/config    {:profile :prod}
   :ulog/publisher   {:config (ig/ref :system/config)}
   :db/connection    {:config (ig/ref :system/config)}
   :http/router-opts {:config (ig/ref :system/config)
                      :db     (ig/ref :db/connection)}
   :http/router-prod {:router-opts (ig/ref :http/router-opts)}
   :http/handler     {:router (ig/ref :http/router-prod)}
   :http/adapter     {:handler (ig/ref :http/handler)
                      :config  (ig/ref :system/config)}})

(defmethod ig/init-key :system/config
  [_ {:keys [profile]}]
  (u/set-global-context!
   {:app-name "education-api"
    :version  "1.6.0"
    :env      profile})
  (u/log ::read-configuration :profile profile)
  (config/config profile))

(defmethod ig/init-key :ulog/publisher
  [_ {:keys [config]}]
  (u/start-publisher!
   {:type       :multi
    :publishers (config/ulog-publishers config)}))

(defmethod ig/halt-key! :ulog/publisher
  [_ pub]
  (pub))

(defmethod ig/init-key :db/connection
  [_ {:keys [config]}]
  (extend-protocol rs/ReadableColumn
    Array
    (read-column-by-label [^Array v _] (vec (.getArray v)))
    (read-column-by-index [^Array v _ _] (vec (.getArray v))))
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
    (u/log ::start-http-kit :port port)
    (server/run-server handler {:port (config/application-port config)})))

(defmethod ig/halt-key! :http/adapter
  [_ srv]
  (u/log ::stop-http-kit)
  (srv :timeout 100))
