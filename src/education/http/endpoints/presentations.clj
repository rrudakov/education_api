(ns education.http.endpoints.presentations
  (:require
   [education.database.presentations :as presentations-db]
   [education.http.constants :as const]
   [education.utils.maps :refer [remove-nils unqualify-map]]
   [ring.util.http-response :as status]))

;;; Handlers

(defn create-presentation-handler
  [{:keys [conn] {:keys [body]} :parameters}]
  (let [presentation-id (presentations-db/add-presentation conn body)]
    (status/created (str "/presentations/" presentation-id) {:id presentation-id})))

(defn update-presesntation-handler
  [{:keys [conn] {:keys [body] {:keys [presentation-id]} :path} :parameters}]
  (case (presentations-db/update-presentation conn presentation-id body)
    1 (status/no-content)
    0 (status/not-found {:message const/not-found-error-message})
    (status/internal-server-error {:message const/server-error-message})))

(defn get-presentation-by-id-handler
  [{:keys [conn] {{:keys [presentation-id]} :path} :parameters}]
  (if-let [presentation (presentations-db/get-presentation-by-id conn presentation-id)]
    (status/ok (-> presentation unqualify-map remove-nils))
    (status/not-found {:message const/not-found-error-message})))

(defn get-all-presentations-handler
  [{:keys [conn] {{:keys [subtype_id limit offset]} :query} :parameters}]
  (->> (presentations-db/get-all-presentations conn subtype_id :limit limit :offset offset)
       (into [] (comp
                 (map unqualify-map)
                 (map remove-nils)))
       (status/ok)))

(defn delete-presentation-by-id-handler
  [{:keys [conn] {{:keys [presentation-id]} :path} :parameters}]
  (case (presentations-db/delete-presentation conn presentation-id)
    1 (status/no-content)
    0 (status/not-found {:message const/not-found-error-message})
    (status/internal-server-error {:message const/server-error-message})))
