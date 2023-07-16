(ns education.database.materials
  (:require
   [cljc.java-time.instant :as instant]
   [education.utils.maps :refer [update-if-exist]]
   [next.jdbc.sql :as sql]))

(defn- ->db-model-create
  [material]
  (update material :price bigdec))

(defn- ->db-model-update
  [material]
  (-> material
      (assoc :updated_on (instant/now))
      (update-if-exist :price bigdec)))

(defn add-material
  "Create new `material` in database with given `conn`."
  [conn material]
  (let [query (->db-model-create material)]
    (-> (sql/insert! conn :materials query)
        :materials/id)))

(defn update-material
  "Update existing `material` by `material-id` with given `conn`."
  [conn material-id material]
  (let [query (->db-model-update material)]
    (-> (sql/update! conn :materials query {:id material-id})
        :next.jdbc/update-count)))

(defn get-material-by-id
  "Get existing material from database by `material-id` with given `conn`."
  [conn material-id]
  (sql/get-by-id conn :materials material-id))

(defn get-all-materials
  "Get all materials from database."
  [conn & {:keys [limit offset]}]
  (sql/find-by-keys conn :materials :all
                    {:limit  (or limit 20)
                     :offset (or offset 0)}))

(defn delete-material
  "Delete material by `material-id` with given `conn`."
  [conn material-id]
  (-> (sql/delete! conn :materials {:id material-id})
      :next.jdbc/update-count))
