(ns education.http.endpoints.lessons
  (:require [compojure.api.sweet :refer [context DELETE GET PATCH POST]]
            [education.database.lessons :as lessons-db]
            [education.http.constants :as const]
            [education.http.restructure :refer [require-roles]]
            [education.specs.error :as err]
            [education.specs.lessons :as specs]
            [education.utils.maps :refer [unqualify-map]]
            [ring.util.http-response :as status]))

;; Helper functions
(defn- db->response
  "Convert database query result to response."
  [db-lesson]
  (update (unqualify-map db-lesson) :price (partial format "%.2f")))

;; Handlers
(defn- create-lesson-handler
  "Create new lesson handler."
  [db lesson]
  (let [lesson-id (lessons-db/add-lesson db lesson)]
    (status/created (str "/lessons/" lesson-id) {:id lesson-id})))

(defn- update-lesson-handler
  "Update existing lesson handler."
  [db lesson-id lesson]
  (case (lessons-db/update-lesson db lesson-id lesson)
    1 (status/no-content)
    0 (status/not-found {:message const/not-found-error-message})
    (status/internal-server-error {:message const/server-error-message})))

(defn- get-lesson-by-id-handler
  "Get lesson by `lesson-id` handler."
  [db lesson-id]
  (if-let [lesson (lessons-db/get-lesson-by-id db lesson-id)]
    (status/ok (unqualify-map lesson))
    (status/not-found {:message const/not-found-error-message})))

(defn- get-all-lessons-handler
  "Get list of lessons with optional `offset` and `limit` handler."
  [db limit offset]
  (status/ok (mapv db->response (lessons-db/get-all-lessons db :limit limit :offset offset))))

(defn- delete-lesson-by-id-handler
  "Delete lesson by `lesson-id` handler."
  [db lesson-id]
  (case (lessons-db/delete-lesson db lesson-id)
    1 (status/no-content)
    0 (status/not-found {:message const/not-found-error-message})
    (status/internal-server-error {:message const/not-found-error-message})))

(defn lessons-routes
  "Define routes for lessons endpoint."
  [db]
  (context "/lessons" []
    :tags ["lessons"]
    (POST "/" []
      :middleware [[require-roles #{:admin}]]
      :body [lesson ::specs/lesson-create-request]
      :return ::specs/id
      :summary "Create new lesson"
      :responses {401 {:description const/not-authorized-error-message
                       :schema      ::err/error-response}
                  403 {:description const/no-access-error-message
                       :schema      ::err/error-response}
                  400 {:description const/bad-request-error-message
                       :schema      ::err/error-response}}
      (create-lesson-handler db lesson))
    (PATCH "/:lesson-id" []
      :middleware [[require-roles #{:admin}]]
      :body [lesson ::specs/lesson-update-request]
      :path-params [lesson-id :- ::specs/id]
      :summary "Update existing lesson by `lesson-id`"
      :responses {401 {:description const/not-authorized-error-message
                       :schema      ::err/error-response}
                  403 {:description const/no-access-error-message
                       :schema      ::err/error-response}
                  404 {:description const/not-found-error-message
                       :schema      ::err/error-response}
                  400 {:description const/bad-request-error-message
                       :schema      ::err/error-response}}
      (update-lesson-handler db lesson-id lesson))
    (GET "/:lesson-id" []
      :path-params [lesson-id :- ::specs/id]
      :summary "Get lesson by `lesson-id`"
      :responses {404 {:description const/not-found-error-message
                       :schema      ::err/error-response}
                  400 {:description const/bad-request-error-message
                       :schema      ::err/error-response}}
      (get-lesson-by-id-handler db lesson-id))
    (GET "/" []
      :return ::specs/lessons-response
      :query-params [{limit :- ::specs/limit nil}
                     {offset :- ::specs/offset nil}]
      :summary "Get list of lessons with given `limit` and `offset`."
      :responses {400 {:description const/bad-request-error-message
                       :schema      ::err/error-response}}
      (get-all-lessons-handler db limit offset))
    (DELETE "/:lesson-id" []
      :middleware [[require-roles #{:admin}]]
      :path-params [lesson-id :- ::specs/id]
      :summary "Delete lesson by `lesson-id`"
      :responses {404 {:description const/not-found-error-message
                       :schema      ::err/error-response}
                  401 {:description const/not-authorized-error-message
                       :schema      ::err/error-response}
                  403 {:description const/no-access-error-message
                       :schema      ::err/error-response}
                  400 {:description const/bad-request-error-message
                       :schema      ::err/error-response}}
      (delete-lesson-by-id-handler db lesson-id))))
