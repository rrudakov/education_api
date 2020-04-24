(ns ring-learn.http.routes
  (:require [buddy.auth :refer [authenticated?]]
            [buddy.auth.accessrules :refer [restrict]]
            [compojure.api.meta :refer [restructure-param]]
            [compojure.api.sweet :refer [api context GET]]
            [ring-learn.http.endpoints.users :refer [users-routes]]
            [ring-learn.middleware :as mw]
            [ring.util.http-response
             :refer
             [conflict internal-server-error ok unauthorized]]
            [schema.core :as s])
  (:import org.postgresql.util.PSQLException))

;; TODO: Move all this shit to separate namespace
(defn sql-exception-handler
  "Database exception mapper."
  []
  (fn [^PSQLException e data request]
    (case (.getSQLState e)
      "23505" (conflict {:message "Resource already exist"})
      (internal-server-error {:message (.getServerErrorMessage e)
                              :errorCode (.getSQLState e)}))))

(defn access-error
  [handler val]
  (unauthorized {:message "You are not authorized!"}))

(defn wrap-restricted
  [handler rule]
  (restrict handler {:handler rule
                     :on-error access-error}))

(defmethod restructure-param :auth-rules
  [_ rule acc]
  (update-in acc [:middleware] conj [wrap-restricted rule]))

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
       :auth-rules authenticated?
       :return {:result Long}
       :header-params [authorization :- s/Str]
       :query-params [x :- Long
                      y :- Long]
       :summary "Adds two numbers together"
       (ok {:result (+ x y)}))
     (users-routes db config))))
