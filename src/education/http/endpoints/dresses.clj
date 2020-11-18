(ns education.http.endpoints.dresses
  (:require [compojure.api.sweet :refer [context PATCH POST GET DELETE]]
            [education.database.dresses :as dresses-db]
            [education.http.constants :as const]
            [education.http.restructure :refer [require-roles]]
            [education.specs.dresses :as specs]
            [education.specs.error :as err]
            [education.utils.maps :refer [unqualify-map]]
            [ring.util.http-response :as status]))

;; Helper functions
(defn- db->response
  "Convert database query result to response."
  [db-dress]
  (update (unqualify-map db-dress) :price (partial format "%.2f")))

;; Handlers
(defn- create-dress-handler
  "Create new dress handler."
  [db dress]
  (let [dress-id (dresses-db/add-dress db dress)]
    (status/created (str "/dresses/" dress-id) {:id dress-id})))

(defn- update-dress-handler
  "Update existing dress handler."
  [db dress-id dress]
  (case (dresses-db/update-dress db dress-id dress)
    1 (status/no-content)
    0 (status/not-found {:message const/not-found-error-message})
    (status/internal-server-error {:message const/server-error-message})))

(defn- get-dress-by-id-handler
  "Get dress by `dress-id` handler."
  [db dress-id]
  (if-let [dress (dresses-db/get-dress-by-id db dress-id)]
    (status/ok (db->response dress))
    (status/not-found {:message const/not-found-error-message})))

(defn- get-all-dresses-handler
  "Get list of dresses with optional `offset` and `limit` handler."
  [db limit offset]
  (status/ok (mapv db->response (dresses-db/get-all-dresses db :limit limit :offset offset))))

(defn- delete-dress-by-id-handler
  "Delete dress by `dress-id` handler."
  [db dress-id]
  (case (dresses-db/delete-dress db dress-id)
    1 (status/no-content)
    0 (status/not-found {:message const/not-found-error-message})
    (status/internal-server-error {:message const/server-error-message})))

;; Define routes
(defn dresses-routes
  "Define routes for dresses endpoint."
  [db]
  (context "/dresses" []
    :tags ["dresses"]
    (POST "/" []
      :middleware [[require-roles #{:admin}]]
      :body [dress ::specs/dress-create-request]
      :summary "Create new dress"
      :responses {201 {:description "Dress created successfully"
                       :schema      ::specs/dress-create-response}
                  400 {:description const/bad-request-error-message
                       :schema      ::err/error-response}
                  401 {:description const/not-authorized-error-message
                       :schema      ::err/error-response}
                  403 {:description const/no-access-error-message
                       :schema      ::err/error-response}}
      (create-dress-handler db dress))
    (PATCH "/:dress-id" []
      :middleware [[require-roles #{:admin}]]
      :body [dress ::specs/dress-update-request]
      :path-params [dress-id :- ::specs/id]
      :summary "Update dress entry"
      :description "Update existing dress by `dress-id`"
      :responses {400 {:description const/bad-request-error-message
                       :schema      ::err/error-response}
                  401 {:description const/not-authorized-error-message
                       :schema      ::err/error-response}
                  403 {:description const/no-access-error-message
                       :schema      ::err/error-response}
                  404 {:description const/not-found-error-message
                       :schema      ::err/error-response}}
      (update-dress-handler db dress-id dress))
    (GET "/:dress-id" []
      :path-params [dress-id :- ::specs/id]
      :summary "Get dress entry"
      :description "Get dress by `dress-id`"
      :responses {200 {:description "Dress was successfully found in the database"
                       :schema      ::specs/dress-response}
                  400 {:description const/bad-request-error-message
                       :schema      ::err/error-response}
                  404 {:description const/not-found-error-message
                       :schema      ::err/error-response}}
      (get-dress-by-id-handler db dress-id))
    (GET "/" []
      :query-params [{limit :- ::specs/limit nil}
                     {offset :- ::specs/offset nil}]
      :summary "Get all dresses"
      :description "Get list of dresses with given optional `limit` and `offset`"
      :responses {200 {:description "Successful response"
                       :schema      ::specs/dresses-response}
                  400 {:description const/bad-request-error-message
                       :schema      ::err/error-response}}
      (get-all-dresses-handler db limit offset))
    (DELETE "/:dress-id" []
      :middleware [[require-roles #{:admin}]]
      :path-params [dress-id :- ::specs/id]
      :summary "Delete dress entry"
      :description "Delete dress entry by `dress-id`"
      :responses {400 {:description const/bad-request-error-message
                       :schema      ::err/error-response}
                  401 {:description const/not-authorized-error-message
                       :schema      ::err/error-response}
                  403 {:description const/no-access-error-message
                       :schema      ::err/error-response}
                  404 {:description const/not-found-error-message
                       :schema      ::err/error-response}}
      (delete-dress-by-id-handler db dress-id))))
