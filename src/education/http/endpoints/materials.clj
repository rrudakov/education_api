(ns education.http.endpoints.materials
  (:require [compojure.api.core :refer [context DELETE GET PATCH POST]]
            [education.database.materials :as materials-db]
            [education.database.materials-orders :as materials-orders-db]
            [education.database.orders :as orders-db]
            [education.http.constants :as const]
            [education.http.restructure :refer [require-roles]]
            [education.specs.common :as spec]
            [education.utils.maps :refer [unqualify-map]]
            [education.utils.stripe :as stripe]
            [ring.util.http-response :as status]
            [taoensso.timbre :refer [info infof]]))

;;; Helper functions

(defn- ->response
  "Convert database query result to response."
  [db-material]
  (-> db-material
      (unqualify-map)
      (update :price (partial format "%.2f"))))

;;; Handlers

(defn- create-material-handler
  "Create new `material` handler."
  [conn material]
  (let [material-id (materials-db/add-material conn material)]
    (info "New material with ID " material-id " was successfully created")
    (status/created (str "/materials/" material-id) {:id material-id})))

(defn- update-material-handler
  "Update existing material handler."
  [conn material-id material]
  (case (materials-db/update-material conn material-id material)
    1 (status/no-content)
    0 (status/not-found {:message const/not-found-error-message})
    (status/internal-server-error {:message const/server-error-message})))

(defn- get-material-by-id-handler
  "Get material by `material-id` handler."
  [conn material-id]
  (if-let [material (materials-db/get-material-by-id conn material-id)]
    (status/ok (->response material))
    (status/not-found {:message const/not-found-error-message})))

(defn- checkout-material-by-id-handler
  "Return payment session for material by `material-id` handler."
  [conn config material-id]
  (if-let [{:materials/keys [title price]} (materials-db/get-material-by-id conn material-id)]
    (let [session-id (stripe/create-checkout-session config title price)]
      (infof "Stripe session id: %s" session-id)
      (orders-db/create-order conn session-id :materials)
      (materials-orders-db/create-material-order conn session-id material-id)
      (status/ok {:id session-id}))
    (status/not-found {:message const/not-found-error-message})))

(defn- get-all-materials-handler
  "Get list of materials with optional `offset` and `limit` handler."
  [conn limit offset]
  (->> (materials-db/get-all-materials conn :limit limit :offset offset)
       (mapv ->response)
       (status/ok)))

(defn- delete-material-by-id-handler
  "Delete material by `material-id` handler."
  [conn material-id]
  (case (materials-db/delete-material conn material-id)
    1 (status/no-content)
    0 (status/not-found {:message const/not-found-error-message})
    (status/internal-server-error {:message const/server-error-message})))

;;; Routes

(defn materials-routes
  "Define routes for materials API."
  [db config]
  (context "/materials" []
    :tags ["materials"]
    (POST "/" []
      :middleware [[require-roles #{:admin}]]
      :body [material ::spec/material-create-request]
      :summary "Create new material"
      :responses {201 {:description "Material created successfully"
                       :schema      ::spec/create-response}
                  400 {:description const/bad-request-error-message
                       :schema      ::spec/error-response}
                  401 {:description const/not-authorized-error-message
                       :schema      ::spec/error-response}
                  403 {:description const/no-access-error-message
                       :schema      ::spec/error-response}}
      (create-material-handler db material))
    (PATCH "/:material-id" []
      :middleware [[require-roles #{:admin}]]
      :body [material ::spec/material-update-request]
      :path-params [material-id :- ::spec/id]
      :summary "Update material entry"
      :description "Update existing material by `material-id`"
      :responses {400 {:description const/bad-request-error-message
                       :schema      ::spec/error-response}
                  401 {:description const/not-authorized-error-message
                       :schema      ::spec/error-response}
                  403 {:description const/no-access-error-message
                       :schema      ::spec/error-response}
                  404 {:description const/not-found-error-message
                       :schema      ::spec/error-response}}
      (update-material-handler db material-id material))
    (GET "/:material-id" []
      :path-params [material-id :- ::spec/id]
      :summary "Get material entry"
      :description "Get material by `material-id`"
      :responses {200 {:description "Material was successfully found in the database"
                       :schema      ::spec/material-response}
                  400 {:description const/bad-request-error-message
                       :schema      ::spec/error-response}
                  404 {:description const/not-found-error-message
                       :schema      ::spec/error-response}}
      (get-material-by-id-handler db material-id))
    (POST "/:material-id/checkout" []
      :path-params [material-id :- ::spec/id]
      :summary "Get payment session"
      :description "Get payment session for material by `material-id`"
      :responses {200 {:description "Material was found and checkout session was created successfully"
                       :schema      ::spec/create-checkout-session-response}
                  400 {:description const/bad-request-error-message
                       :schema      ::spec/error-response}
                  404 {:description const/not-found-error-message
                       :schema      ::spec/error-response}}
      (checkout-material-by-id-handler db config material-id))
    (GET "/" []
      :query-params [{limit :- ::spec/limit nil}
                     {offset :- ::spec/offset nil}]
      :summary "Get all materials"
      :description "Get list of materials with given optional `limit` and `offset`"
      :responses {200 {:description "Successful response"
                       :schema      ::spec/materials-response}
                  400 {:description const/bad-request-error-message
                       :schema      ::spec/error-response}}
      (get-all-materials-handler db limit offset))
    (DELETE "/:material-id" []
      :middleware [[require-roles #{:admin}]]
      :path-params [material-id :- ::spec/id]
      :summary "Delete material entry"
      :description "Delete material entry by `material-id`"
      :responses {400 {:description const/bad-request-error-message
                       :schema      ::spec/error-response}
                  401 {:description const/not-authorized-error-message
                       :schema      ::spec/error-response}
                  403 {:description const/no-access-error-message
                       :schema      ::spec/error-response}
                  404 {:description const/not-found-error-message
                       :schema      ::spec/error-response}}
      (delete-material-by-id-handler db material-id))))
