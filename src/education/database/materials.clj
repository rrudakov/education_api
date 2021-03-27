(ns education.database.materials
  (:require [education.utils.maps :refer [update-if-exist]]
            [next.jdbc.sql :as sql])
  (:import java.time.Instant))

(defn- ->db-model-create
  [material]
  (-> material
      (update :price bigdec)))

(defn- ->db-model-update
  [material]
  (-> material
      (assoc :update_on (Instant/now))
      (update-if-exist :price bigdec)))

(defn add-material
  "Create new `material` in database with given `conn`."
  [conn material]
  (->> material
       (->db-model-create)
       (sql/insert! conn :materials)
       (:materials/id)))

(defn update-material
  "Update existing `material` by `material-id` with given `conn`."
  [conn material-id material]
  (->> {:id material-id}
       (sql/update! conn :materials (->db-model-update material))
       (:next.jdbc/update-count)))

(defn get-material-by-id
  "Get existing material from database by `material-id` with given `conn`."
  [conn material-id]
  (sql/get-by-id conn :materials material-id))

(defn get-all-materials
  "Get all materials from database."
  [conn & {:keys [limit offset]}]
  (sql/find-by-keys conn :materials
                    {:limit  (or limit 20)
                     :offset (or offset 0)}))

(defn delete-material
  "Delete material by `material-id` with given `conn`."
  [conn material-id]
  (->> (sql/delete! conn :materials {:id material-id})
       (:next.jdbc/update-count)))
