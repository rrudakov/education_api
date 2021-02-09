(ns education.http.endpoints.gymnastics
  (:require [compojure.api.sweet :refer [context DELETE GET PATCH POST]]
            [education.database.gymnastics :as gymnastics-db]
            [education.http.constants :as const]
            [education.http.restructure :refer [require-roles]]
            [education.specs.common :as spec]
            [education.utils.maps :refer [unqualify-map]]
            [ring.util.http-response :as status]))

;; Handlers
(defn- create-gymnastic-handler
  [db gymnastic]
  (let [gymnastic-id (gymnastics-db/add-gymnastic db gymnastic)]
    (status/created (str "/gymnastics/" gymnastic-id) {:id gymnastic-id})))

(defn- update-gymnastic-handler
  [db gymnastic-id gymnastic]
  (case (gymnastics-db/update-gymnastic db gymnastic-id gymnastic)
    1 (status/no-content)
    0 (status/not-found {:message const/not-found-error-message})
    (status/internal-server-error {:message const/server-error-message})))

(defn- get-gymnastic-by-id-handler
  [db gymnastic-id]
  (if-let [gymnastic (gymnastics-db/get-gymnastic-by-id db gymnastic-id)]
    (status/ok (unqualify-map gymnastic))
    (status/not-found {:message const/not-found-error-message})))

(defn- get-all-gymnastics-handler
  [db subtype-id limit offset]
  (->> (gymnastics-db/get-all-gymnastics db subtype-id :limit limit :offset offset)
       (mapv unqualify-map)
       (status/ok)))

(defn- delete-gymnastic-by-id-handler
  [db gymnastic-id]
  (case (gymnastics-db/delete-gymnastic db gymnastic-id)
    1 (status/no-content)
    0 (status/not-found {:message const/not-found-error-message})
    (status/internal-server-error {:message const/server-error-message})))

;; Define routes
(defn gymnastics-routes
  "Define routes for gymnastics API."
  [db]
  (context "/gymnastics" []
    :tags ["gymnastics"]
    (POST "/" []
      :middleware [[require-roles #{:admin}]]
      :body [gymnastic ::spec/gymnastic-create-request]
      :summary "Create new gymnastic"
      :responses {201 {:description "Gymnastic created successfully"
                       :schema      ::spec/create-response}
                  400 {:description const/bad-request-error-message
                       :schema      ::spec/error-response}
                  401 {:description const/not-authorized-error-message
                       :schema      ::spec/error-response}
                  403 {:description const/no-access-error-message
                       :schema      ::spec/error-response}}
      (create-gymnastic-handler db gymnastic))
    (PATCH "/:gymnastic-id" []
      :middleware [[require-roles #{:admin}]]
      :body [gymnastic ::spec/gymnastic-update-request]
      :path-params [gymnastic-id :- ::spec/id]
      :summary "Update gymnastic entry"
      :description "Update existing gymnastic by `gymnastic-id`"
      :responses {400 {:description const/bad-request-error-message
                       :schema      ::spec/error-response}
                  401 {:description const/not-authorized-error-message
                       :schema      ::spec/error-response}
                  403 {:description const/no-access-error-message
                       :schema      ::spec/error-response}
                  404 {:description const/not-found-error-message
                       :schema      ::spec/error-response}}
      (update-gymnastic-handler db gymnastic-id gymnastic))
    (GET "/:gymnastic-id" []
      :path-params [gymnastic-id :- ::spec/id]
      :summary "Get gymnastic entry"
      :description "Get gymnastic by `gymnastic-id`"
      :responses {200 {:description "Gymnastic was successfully found in the database"
                       :schema      ::spec/gymnastic-response}
                  400 {:description const/bad-request-error-message
                       :schema      ::spec/error-response}
                  404 {:description const/not-found-error-message
                       :schema      ::spec/error-response}}
      (get-gymnastic-by-id-handler db gymnastic-id))
    (GET "/" []
      :query-params [subtype_id :- ::spec/subtype_id
                     {limit :- ::spec/limit nil}
                     {offset :- ::spec/offset nil}]
      :summary "Get all gymnastics"
      :description "Get list of gymnastics with given optional `limit` and `offset`"
      :responses {200 {:description "Successful response"
                       :schema      ::spec/gymnastics-response}
                  400 {:description const/bad-request-error-message
                       :schema      ::spec/error-response}}
      (get-all-gymnastics-handler db subtype_id limit offset))
    (DELETE "/:gymnastic-id" []
      :middleware [[require-roles #{:admin}]]
      :path-params [gymnastic-id :- ::spec/id]
      :summary "Delete gymnastic entry"
      :description "Delete gymnastic entry by `gymnastic-id`"
      :responses {400 {:description const/bad-request-error-message
                       :schema      ::spec/error-response}
                  401 {:description const/not-authorized-error-message
                       :schema      ::spec/error-response}
                  403 {:description const/no-access-error-message
                       :schema      ::spec/error-response}
                  404 {:description const/not-found-error-message
                       :schema      ::spec/error-response}}
      (delete-gymnastic-by-id-handler db gymnastic-id))))
