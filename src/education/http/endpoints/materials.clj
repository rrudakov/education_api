(ns education.http.endpoints.materials
  (:require
   [com.brunobonacci.mulog :as u]
   [education.database.materials :as materials-db]
   [education.http.constants :as const]
   [education.utils.maps :refer [unqualify-map]]
   [ring.util.http-response :as status]))

;;; Helper functions

(defn- ->response
  "Convert database query result to response."
  [db-material]
  (-> db-material
      (unqualify-map)
      (update :price (partial format "%.2f"))))

;;; Handlers

(defn create-material-handler
  "Create new `material` handler."
  [{:keys [conn] {:keys [body]} :parameters}]
  (let [material-id (materials-db/add-material conn body)]
    (u/log ::create-material :material-id material-id)
    (status/created (str "/materials/" material-id) {:id material-id})))

(defn update-material-handler
  "Update existing material handler."
  [{:keys [conn] {:keys [body] {:keys [material-id]} :path} :parameters}]
  (case (materials-db/update-material conn material-id body)
    1 (status/no-content)
    0 (status/not-found {:message const/not-found-error-message})
    (status/internal-server-error {:message const/server-error-message})))

(defn get-material-by-id-handler
  "Get material by `material-id` handler."
  [{:keys [conn] {{:keys [material-id]} :path} :parameters}]
  (if-let [material (materials-db/get-material-by-id conn material-id)]
    (status/ok (->response material))
    (status/not-found {:message const/not-found-error-message})))

(defn get-all-materials-handler
  "Get list of materials with optional `offset` and `limit` handler."
  [{:keys [conn] {{:keys [limit offset]} :query} :parameters}]
  (->> (materials-db/get-all-materials conn :limit limit :offset offset)
       (mapv ->response)
       (status/ok)))

(defn delete-material-by-id-handler
  "Delete material by `material-id` handler."
  [{:keys [conn] {{:keys [material-id]} :path} :parameters}]
  (case (materials-db/delete-material conn material-id)
    1 (status/no-content)
    0 (status/not-found {:message const/not-found-error-message})
    (status/internal-server-error {:message const/server-error-message})))
