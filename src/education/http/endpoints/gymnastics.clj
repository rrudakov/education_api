(ns education.http.endpoints.gymnastics
  (:require
   [education.database.gymnastics :as gymnastics-db]
   [education.http.constants :as const]
   [education.utils.maps :refer [unqualify-map]]
   [ring.util.http-response :as status]))

;;; Handlers

(defn create-gymnastic-handler
  [{:keys [conn] {:keys [body]} :parameters}]
  (let [gymnastic-id (gymnastics-db/add-gymnastic conn body)]
    (status/created (str "/gymnastics/" gymnastic-id) {:id gymnastic-id})))

(defn update-gymnastic-handler
  [{:keys [conn] {:keys [body] {:keys [gymnastic-id]} :path} :parameters}]
  (let [data (update body :picture identity)]
    (case (gymnastics-db/update-gymnastic conn gymnastic-id data)
      1 (status/no-content)
      0 (status/not-found {:message const/not-found-error-message})
      (status/internal-server-error {:message const/server-error-message}))))

(defn get-gymnastic-by-id-handler
  [{:keys [conn] {{:keys [gymnastic-id]} :path} :parameters}]
  (if-let [gymnastic (gymnastics-db/get-gymnastic-by-id conn gymnastic-id)]
    (status/ok (unqualify-map gymnastic))
    (status/not-found {:message const/not-found-error-message})))

(defn get-all-gymnastics-handler
  [{:keys [conn] {{:keys [subtype_id limit offset]} :query} :parameters}]
  (->> (gymnastics-db/get-all-gymnastics conn subtype_id :limit limit :offset offset)
       (into [] (map unqualify-map))
       (status/ok)))

(defn delete-gymnastic-by-id-handler
  [{:keys [conn] {{:keys [gymnastic-id]} :path} :parameters}]
  (case (gymnastics-db/delete-gymnastic conn gymnastic-id)
    1 (status/no-content)
    0 (status/not-found {:message const/not-found-error-message})
    (status/internal-server-error {:message const/server-error-message})))
