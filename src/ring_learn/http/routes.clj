(ns ring-learn.http.routes
  (:require [compojure.api.sweet :refer [api context GET]]
            [ring-learn.http.endpoints.users :refer [users-routes]]
            [ring.util.http-response :refer [conflict internal-server-error ok]])
  (:import org.postgresql.util.PSQLException))

(defn sql-exception-handler
  "Database exception mapper."
  []
  (fn [^PSQLException e data request]
    (case (.getSQLState e)
      "23505" (conflict {:message "Resource already exist"})
      (internal-server-error {:message (.getServerErrorMessage e)
                              :errorCode (.getSQLState e)}))))

(defn api-routes
  "Define top-level API routes."
  [db config]
  (api
   {:swagger
    {:ui "/"
     :spec "/swagger.json"
     :data {:info {:title "My first clojure API"
                   :description "Some endpoints for simple application"}
            :tags [{:name "api" :description "REST API"}]}}

    :exceptions
    {:handlers
     {PSQLException (sql-exception-handler)}}}

   (context "/api" []
     :tags ["api"]

     (GET "/plus" []
       :return {:result Long}
       :query-params [x :- Long
                      y :- Long]
       :summary "Adds two numbers together"
       (ok {:result (+ x y)}))
     (users-routes db config))))
