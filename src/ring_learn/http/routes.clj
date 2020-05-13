(ns ring-learn.http.routes
  (:require [compojure.api.sweet :refer [api context]]
            [ring-learn.http.endpoints.articles :refer [articles-routes]]
            [ring-learn.http.endpoints.roles :refer [roles-routes]]
            [ring-learn.http.endpoints.users :refer [users-routes]]
            [ring.util.http-response
             :refer
             [bad-request conflict internal-server-error not-found]])
  (:import org.postgresql.util.PSQLException))

(defn sql-exception-handler
  "Database exception mapper."
  []
  (fn [^PSQLException e data request]
    (case (.getSQLState e)
      "23505" (conflict {:message "Resource already exist"})
      "23503" (not-found {:message "Resource not found"})
      "23502" (bad-request {:message "Bad request"})
      (internal-server-error {:message (.getServerErrorMessage e)
                              :errorCode (.getSQLState e)}))))

(defn api-routes
  "Define top-level API routes."
  [{:keys [datasource]} config]
  (api
   {:swagger
    {:ui "/swagger"
     :spec "/swagger.json"
     :options {:ui {:jsonEditor true}
               :spec {}}
     :data {:info {:version "0.1-alpha"
                   :title "Education API"
                   :description "REST API for education application"
                   :contact {:name "Roman Rudakov"
                             :email "rrudakov@pm.me"}}
            :tags [{:name "users" :description "Users management"}
                   {:name "roles" :description "Roles management"}
                   {:name "articles" :description "Articles management"}]
            :securityDefinitions {:api_key {:type "apiKey" :name "Authorization" :in "header"}}}}
    :exceptions
    {:handlers
     {PSQLException (sql-exception-handler)}}}

   (context "/api" []
     (users-routes datasource config)
     (articles-routes datasource)
     (roles-routes datasource))))
