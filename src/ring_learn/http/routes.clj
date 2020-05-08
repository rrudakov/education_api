(ns ring-learn.http.routes
  (:require [compojure.api.sweet :refer [api context GET]]
            [ring-learn.http.endpoints.roles :refer [roles-routes]]
            [ring-learn.http.endpoints.users :refer [users-routes]]
            [ring-learn.http.restructure :refer [require-roles]]
            [ring.util.http-response
             :refer
             [bad-request conflict internal-server-error ok]])
  (:import org.postgresql.util.PSQLException))

(defn sql-exception-handler
  "Database exception mapper."
  []
  (fn [^PSQLException e data request]
    (case (.getSQLState e)
      "23505" (conflict {:message "Resource already exist"})
      "23502" (bad-request {:message "Bad request"})
      (internal-server-error {:message (.getServerErrorMessage e)
                              :errorCode (.getSQLState e)}))))

(defn- plus-handler
  "Add `x` to `y`."
  [x y]
  (ok {:result (+ x y)}))

(defn api-routes
  "Define top-level API routes."
  [db config]
  (api
   {:swagger
    {:ui "/swagger"
     :spec "/swagger.json"
     :data {:info {:title "My first clojure API"
                   :description "Some endpoints for simple application"}
            :tags [{:name "plus" :description "REST API to add two numbers"}
                   {:name "users" :description "Users management"}
                   {:name "roles" :description "Roles management"}]
            :securityDefinitions {:api_key {:type "apiKey" :name "Authorization" :in "header"}}}}
    :exceptions
    {:handlers
     {PSQLException (sql-exception-handler)}}}

   (context "/api" []
     :tags ["plus"]

     (GET "/plus" []
       :middleware [[require-roles #{:moderator}]]
       :return {:result Long}
       :query-params [x :- Long
                      y :- Long]
       :summary "Adds two numbers together"
       (plus-handler x y))
     (users-routes db config)
     (roles-routes db))))
