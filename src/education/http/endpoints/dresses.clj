(ns education.http.endpoints.dresses
  (:require
   [com.brunobonacci.mulog :as u]
   [education.database.dresses :as dresses-db]
   [education.http.constants :as const]
   [education.utils.maps :refer [unqualify-map]]
   [ring.util.http-response :as status]))

;; Helper functions
(defn- db->response
  "Convert database query result to response."
  [db-dress]
  (update (unqualify-map db-dress) :price (partial format "%.2f")))

;; Handlers
(defn create-dress-handler
  "Create new dress handler."
  [{:keys [conn] {:keys [body]} :parameters}]
  (let [dress-id (dresses-db/add-dress conn body)]
    (u/log ::dress-created :dress-id dress-id)
    (status/created (str "/dresses/" dress-id) {:id dress-id})))

(defn update-dress-handler
  "Update existing dress handler."
  [{:keys [conn] {:keys [body] {:keys [dress-id]} :path} :parameters}]
  (case (dresses-db/update-dress conn dress-id body)
    1 (status/no-content)
    0 (status/not-found {:message const/not-found-error-message})
    (status/internal-server-error {:message const/server-error-message})))

(defn get-dress-by-id-handler
  "Get dress by `dress-id` handler."
  [{:keys [conn] {{:keys [dress-id]} :path} :parameters}]
  (if-let [dress (dresses-db/get-dress-by-id conn dress-id)]
    (status/ok (db->response dress))
    (status/not-found {:message const/not-found-error-message})))

(defn get-all-dresses-handler
  "Get list of dresses with optional `offset` and `limit` handler."
  [{:keys [conn] {{:keys [limit offset]} :query} :parameters}]
  (->> (dresses-db/get-all-dresses conn :limit limit :offset offset)
       (mapv db->response)
       (status/ok)))

(defn delete-dress-by-id-handler
  "Delete dress by `dress-id` handler."
  [{:keys [conn] {{:keys [dress-id]} :path} :parameters}]
  (case (dresses-db/delete-dress conn dress-id)
    1 (status/no-content)
    0 (status/not-found {:message const/not-found-error-message})
    (status/internal-server-error {:message const/server-error-message})))
