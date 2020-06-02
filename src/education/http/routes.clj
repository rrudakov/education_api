(ns education.http.routes
  (:require [compojure.api.exception :as ex]
            [compojure.api.sweet :refer [api context]]
            [education.http.constants :refer :all]
            [education.http.endpoints.articles :refer [articles-routes]]
            [education.http.endpoints.roles :refer [roles-routes]]
            [education.http.endpoints.users :refer [users-routes]]
            [ring.util.http-response
             :refer
             [bad-request conflict internal-server-error not-found]])
  (:import org.postgresql.util.PSQLException))

(defn sql-exception-handler
  "Database exception mapper."
  []
  (fn [^PSQLException e data request]
    (case (.getSQLState e)
      "23505" (conflict {:message conflict-error-message})
      "23503" (not-found {:message not-found-error-message})
      "23502" (bad-request {:message bad-request-error-message})
      (internal-server-error {:message (.getServerErrorMessage e)
                              :error_code (.getSQLState e)}))))

(defn request-validation-handler
  "Verify request body and raise error."
  []
  (fn [^Exception e data request]
    (bad-request {:message bad-request-error-message
                  :details (.getMessage e)})))

(defn response-validation-handler
  "Return error in case of invalid response."
  []
  (fn [^Exception e data request]
    (internal-server-error
     {:message server-error-message
      :details (.getMessage e)})))

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
     {PSQLException (sql-exception-handler)
      ::ex/request-validation (request-validation-handler)
      ::ex/response-validation (response-validation-handler)}}}

   (context "/api" []
     :coercion :spec
     (users-routes datasource config)
     (articles-routes datasource)
     (roles-routes datasource))))
