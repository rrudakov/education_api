(ns education.http.routes
  (:require
   [buddy.auth.middleware :refer [wrap-authentication]]
   [education.config :as config]
   [education.database.middleware :as db-mw]
   [education.error :as error]
   [education.http.middleware :as http.middleware]
   [education.http.routes.dresses :as dresses]
   [education.http.routes.gymnastics :as gymnastics]
   [education.http.routes.lessons :as lessons]
   [education.http.routes.materials :as materials]
   [education.http.routes.presentations :as presentations]
   [education.http.routes.roles :as roles]
   [education.http.routes.upload :as upload]
   [education.http.routes.users :as users]
   [muuntaja.core :as m]
   [reitit.coercion.spec :as rcs]
   [reitit.ring.coercion :as rrc]
   [reitit.ring.middleware.multipart :as multipart]
   [reitit.ring.middleware.muuntaja :as muuntaja]
   [reitit.ring.middleware.parameters :as parameters]
   [reitit.swagger :as swagger]
   [reitit.swagger-ui :as swagger-ui]
   [ring.middleware.cors :refer [wrap-cors]]))

(def ^:private allowed-headers
  ["Origin"
   "Accept"
   "Content-Type"
   "Authorization"
   "X-Requested-With"
   "Cache-Control"])

(defn router-options
  [db config send-grid]
  (let [auth-backend (config/auth-backend config)]
    {:data {:app-config config
            :db         db
            :send-grid  send-grid
            :muuntaja   m/instance
            :coercion   rcs/coercion
            :middleware [[wrap-cors
                          :access-control-allow-origin [#".*"]
                          :access-control-allow-headers allowed-headers
                          :access-control-allow-methods [:get :post :patch :put :delete]]
                         parameters/parameters-middleware
                         muuntaja/format-middleware
                         error/exception-middleware
                         rrc/coerce-response-middleware
                         rrc/coerce-request-middleware
                         multipart/multipart-middleware
                         config/app-config-middleware
                         db-mw/db-middleware
                         http.middleware/wrap-send-grid-client
                         [wrap-authentication auth-backend]]}}))

(defn routes
  []
  [["" {:no-doc    true
        :responses {200 {:schema any?}}
        :swagger
        {:basePath "/"
         :info
         {:title   "Education API"
          :version "1.6.0"}
         :securityDefinitions
         {:api_key
          {:type "apiKey"
           :name "Authorization"
           :in   "header"}}
         :tags     [{:name "users" :description "Users management"}
                    {:name "roles" :description "Roles management"}
                    {:name "lessons" :description "Video lessons"}
                    {:name "dresses" :description "Dresses rent"}
                    {:name "gymnastics" :description "Different gymnastics for children"}
                    {:name "presentations" :description "Interactive presentations"}
                    {:name "upload" :description "Upload media files"}
                    {:name "materials" :description "Download materials for education"}]}}
    ["/swagger.json" {:get (swagger/create-swagger-handler)}]
    ["/swagger/*" {:get (swagger-ui/create-swagger-ui-handler)}]]
   ["/api"
    (users/auth-routes)
    (users/users-routes)
    (roles/roles-routes)
    (lessons/lessons-routes)
    (dresses/dresses-routes)
    (gymnastics/gymnastics-routes)
    (presentations/presentations-routes)
    (materials/materials-routes)
    (upload/upload-routes)]])

(def prod-routes
  (constantly (routes)))
