(ns education.http.endpoints.lessons
  (:require
   [education.config :as config]
   [education.database.email-subscriptions :as email-subscriptions-db]
   [education.database.lessons :as lessons-db]
   [education.http.constants :as const]
   [education.utils.crypt :as crypt]
   [education.utils.mail :as mail]
   [education.utils.maps :refer [unqualify-map]]
   [ring.util.http-response :as status :refer [forbidden]]
   [ring.util.response :refer [file-response]]))

;;; Helper functions

(defn- db->response
  "Convert database query result to response."
  [db-lesson]
  (update (unqualify-map db-lesson) :price (partial format "%.2f")))

;;; Handlers

(defn create-lesson-handler
  "Create new lesson handler."
  [{:keys [conn] {:keys [body]} :parameters}]
  (let [lesson-id (lessons-db/add-lesson conn body)]
    (status/created (str "/lessons/" lesson-id) {:id lesson-id})))

(defn update-lesson-handler
  "Update existing lesson handler."
  [{:keys [conn] {:keys [body] {:keys [lesson-id]} :path} :parameters}]
  (case (lessons-db/update-lesson conn lesson-id body)
    1 (status/no-content)
    0 (status/not-found {:message const/not-found-error-message})
    (status/internal-server-error {:message const/server-error-message})))

(defn get-lesson-by-id-handler
  "Get lesson by `lesson-id` handler."
  [{:keys [conn] {{:keys [lesson-id]} :path} :parameters}]
  (if-let [lesson (lessons-db/get-lesson-by-id conn lesson-id)]
    (status/ok (db->response lesson))
    (status/not-found {:message const/not-found-error-message})))

(defn get-all-lessons-handler
  "Get list of lessons with optional `offset` and `limit` handler."
  [{:keys [conn] {{:keys [limit offset]} :query} :parameters}]
  (->> (lessons-db/get-all-lessons conn :limit limit :offset offset)
       (into [] (map db->response))
       status/ok))

(defn delete-lesson-by-id-handler
  "Delete lesson by `lesson-id` handler."
  [{:keys [conn] {{:keys [lesson-id]} :path} :parameters}]
  (case (lessons-db/delete-lesson conn lesson-id)
    1 (status/no-content)
    0 (status/not-found {:message const/not-found-error-message})
    (status/internal-server-error {:message const/server-error-message})))

(defn request-free-lesson-handler
  [{:keys [conn app-config] {{:keys [email]} :body} :parameters}]
  (let [token (crypt/generate-hash app-config email)]
    (email-subscriptions-db/add-email-subscription conn email)
    (mail/send-free-lesson-email-http app-config token email)
    (status/ok)))

(defn serve-video-handler
  [{:keys [conn app-config] {{:keys [token]} :query} :parameters}]
  (let [email  (crypt/decrypt-hash app-config token)
        exist? (some-> (email-subscriptions-db/get-email-subscription-by-email conn email)
                       (:email_subscriptions/email))]
    (if exist?
      (file-response (config/free-lesson-path app-config)
                     {:root         (config/video-lessons-root-path app-config)
                      :index-files? false})
      (forbidden {:message const/no-access-error-message}))))
