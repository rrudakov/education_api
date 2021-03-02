(ns education.http.endpoints.presentations
  (:require [compojure.api.core :refer [context DELETE GET PATCH POST]]
            [education.database.presentations :as presentations-db]
            [education.http.constants :as const]
            [education.http.restructure :refer [require-roles]]
            [education.specs.common :as spec]
            [education.utils.maps :refer [remove-nils unqualify-map]]
            [ring.util.http-response :as status]))

;; Handlers
(defn- create-presentation-handler
  [db presentation]
  (let [presentation-id (presentations-db/add-presentation db presentation)]
    (status/created (str "/presentations/" presentation-id) {:id presentation-id})))

(defn- update-presesntation-handler
  [db presentation-id presentation]
  (case (presentations-db/update-presentation db presentation-id presentation)
    1 (status/no-content)
    0 (status/not-found {:message const/not-found-error-message})
    (status/internal-server-error {:message const/server-error-message})))

(defn- get-presentation-by-id-handler
  [db presentation-id]
  (if-let [presentation (presentations-db/get-presentation-by-id db presentation-id)]
    (status/ok (-> presentation unqualify-map remove-nils))
    (status/not-found {:message const/not-found-error-message})))

(defn- get-all-presentations-handler
  [db limit offset]
  (->> (presentations-db/get-all-presentations db :limit limit :offset offset)
       (mapv unqualify-map)
       (mapv remove-nils)
       (status/ok)))

(defn- delete-presentation-by-id-handler
  [db presentation-id]
  (case (presentations-db/delete-presentation db presentation-id)
    1 (status/no-content)
    0 (status/not-found {:message const/not-found-error-message})
    (status/internal-server-error {:message const/server-error-message})))

;; Define routes
(defn presentations-routes
  "Define routes for presentations API."
  [db]
  (context "/presentations" []
    :tags ["presentations"]
    (POST "/" []
      :middleware [[require-roles #{:admin}]]
      :body [presentation ::spec/presentation-create-request]
      :summary "Create new presentation"
      :responses {201 {:description "Presentation created successfully"
                       :schema      ::spec/create-response}
                  400 {:description const/bad-request-error-message
                       :schema      ::spec/error-response}
                  401 {:description const/not-authorized-error-message
                       :schema      ::spec/error-response}
                  403 {:description const/no-access-error-message
                       :schema      ::spec/error-response}}
      (create-presentation-handler db presentation))
    (PATCH "/:presentation-id" []
      :middleware [[require-roles #{:admin}]]
      :body [presentation ::spec/presentation-update-request]
      :path-params [presentation-id :- ::spec/id]
      :summary "Update presentation entry"
      :description "Update existing presentations by `presentation-id`"
      :responses {400 {:description const/bad-request-error-message
                       :schema      ::spec/error-response}
                  401 {:description const/not-authorized-error-message
                       :schema      ::spec/error-response}
                  403 {:description const/no-access-error-message
                       :schema      ::spec/error-response}
                  404 {:description const/not-found-error-message
                       :schema      ::spec/error-response}}
      (update-presesntation-handler db presentation-id presentation))
    (GET "/:presentation-id" []
      :middleware [[require-roles #{:admin}]]
      :path-params [presentation-id :- ::spec/id]
      :summary "Get presentation entry"
      :description "Get presentations by `presentation-id`"
      :responses {200 {:description "Presentations was successfully found in the database"
                       :schema      ::spec/presentation-response}
                  400 {:description const/bad-request-error-message
                       :schema      ::spec/error-response}
                  404 {:description const/not-found-error-message
                       :schema      ::spec/error-response}}
      (get-presentation-by-id-handler db presentation-id))
    (GET "/" []
      :query-params [{limit :- ::spec/limit nil}
                     {offset :- ::spec/offset nil}]
      :summary "Get all presentations"
      :description "Get list of presentations with given optionsl `limit` and `offset`"
      :responses {200 {:description "Successful response"
                       :schema      ::spec/presentations-response}
                  400 {:description const/bad-request-error-message
                       :schema      ::spec/error-response}}
      (get-all-presentations-handler db limit offset))
    (DELETE "/:presentation-id" []
      :middleware [[require-roles #{:admin}]]
      :path-params [presentation-id :- ::spec/id]
      :summary "Delete presentation entry"
      :description "Delete presentation by `presentation-id`"
      :responses {400 {:description const/bad-request-error-message
                       :schema      ::spec/error-response}
                  401 {:description const/not-authorized-error-message
                       :schema      ::spec/error-response}
                  403 {:description const/no-access-error-message
                       :schema      ::spec/error-response}
                  404 {:description const/not-found-error-message
                       :schema      ::spec/error-response}}
      (delete-presentation-by-id-handler db presentation-id))))
