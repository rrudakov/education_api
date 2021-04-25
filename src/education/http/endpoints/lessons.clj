(ns education.http.endpoints.lessons
  (:require [compojure.api.core :refer [context DELETE GET PATCH POST routes]]
            [education.config :as config]
            [education.database.email-subscriptions :as email-subscriptions-db]
            [education.database.lessons :as lessons-db]
            [education.http.constants :as const]
            [education.http.restructure :refer [require-roles]]
            [education.specs.common :as spec]
            [education.utils.crypt :as crypt]
            [education.utils.mail :as mail]
            [education.utils.maps :refer [unqualify-map]]
            [ring.util.http-response :as status :refer [forbidden]]
            [ring.util.response :refer [file-response]]))

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
    (status/ok (db->response lesson))
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
    (status/internal-server-error {:message const/server-error-message})))

(defn- request-free-lesson-handler
  [conf db {:keys [email]}]
  (let [token (crypt/generate-hash conf email)]
    (email-subscriptions-db/add-email-subscription db email)
    (mail/send-free-lesson-email-http conf token email)
    (status/ok)))

(defn serve-video-handler
  [conf db token]
  (let [email (crypt/decrypt-hash conf token)
        exist? (some-> (email-subscriptions-db/get-email-subscription-by-email db email)
                       (:email_subscriptions/email))]
    (if exist?
      (file-response (config/free-lesson-path conf)
                     {:root         (config/video-lessons-root-path conf)
                      :index-files? false})
      (forbidden {:message const/no-access-error-message}))))

;; Define routes
(defn lessons-routes
  "Define routes for lessons endpoint."
  [db config]
  (routes
   (GET "/free-lesson" []
     :tags ["lessons"]
     :summary "Get free lesson video"
     :description "Return video file with free video lesson"
     :query-params [token :- string?]
     (serve-video-handler config db token))
   (POST "/free-lesson" []
     :tags ["lessons"]
     :summary "Request free video lesson"
     :description "Request free video lesson by `email`"
     :body [free-lesson-request ::spec/free-lesson-request]
     :responses {400 {:description const/bad-request-error-message
                      :schema      ::spec/error-response}}
     (request-free-lesson-handler config db free-lesson-request))
   (context "/lessons" []
     :tags ["lessons"]
     (POST "/" []
       :middleware [[require-roles #{:admin}]]
       :body [lesson ::spec/lesson-create-request]
       :summary "Create new lesson"
       :responses {201 {:description "Lesson created successfully"
                        :schema      ::spec/create-response}
                   400 {:description const/bad-request-error-message
                        :schema      ::spec/error-response}
                   401 {:description const/not-authorized-error-message
                        :schema      ::spec/error-response}
                   403 {:description const/no-access-error-message
                        :schema      ::spec/error-response}}
       (create-lesson-handler db lesson))
     (PATCH "/:lesson-id" []
       :middleware [[require-roles #{:admin}]]
       :body [lesson ::spec/lesson-update-request]
       :path-params [lesson-id :- ::spec/id]
       :summary "Update lesson"
       :description "Update existing lesson by `lesson-id`"
       :responses {400 {:description const/bad-request-error-message
                        :schema      ::spec/error-response}
                   401 {:description const/not-authorized-error-message
                        :schema      ::spec/error-response}
                   403 {:description const/no-access-error-message
                        :schema      ::spec/error-response}
                   404 {:description const/not-found-error-message
                        :schema      ::spec/error-response}}
       (update-lesson-handler db lesson-id lesson))

     (GET "/:lesson-id" []
       :path-params [lesson-id :- ::spec/id]
       :summary "Get lesson"
       :description "Get lesson by `lesson-id`"
       :responses {200 {:description "User was found in database"
                        :schema      ::spec/lesson-response}
                   400 {:description const/bad-request-error-message
                        :schema      ::spec/error-response}
                   404 {:description const/not-found-error-message
                        :schema      ::spec/error-response}}
       (get-lesson-by-id-handler db lesson-id))
     (GET "/" []
       :query-params [{limit :- ::spec/limit nil}
                      {offset :- ::spec/offset nil}]
       :summary "Get all lessons"
       :description "Get list of lessons with given `limit` and `offset`."
       :responses {200 {:description "Successful"
                        :schema      ::spec/lessons-response}
                   400 {:description const/bad-request-error-message
                        :schema      ::spec/error-response}}
       (get-all-lessons-handler db limit offset))
     (DELETE "/:lesson-id" []
       :middleware [[require-roles #{:admin}]]
       :path-params [lesson-id :- ::spec/id]
       :summary "Delete lesson"
       :description "Delete lesson by `lesson-id`"
       :responses {400 {:description const/bad-request-error-message
                        :schema      ::spec/error-response}
                   401 {:description const/not-authorized-error-message
                        :schema      ::spec/error-response}
                   403 {:description const/no-access-error-message
                        :schema      ::spec/error-response}
                   404 {:description const/not-found-error-message
                        :schema      ::spec/error-response}}
       (delete-lesson-by-id-handler db lesson-id)))))
