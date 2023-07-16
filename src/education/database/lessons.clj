(ns education.database.lessons
  (:require
   [cljc.java-time.instant :as instant]
   [education.utils.maps :refer [update-if-exist]]
   [honey.sql :as hsql]
   [honey.sql.helpers :as h]
   [next.jdbc.date-time]
   [next.jdbc.sql :as sql]))

(defn- request->db-create-statement
  [lesson]
  (-> lesson
      (update :price bigdec)
      (update :screenshots (partial into-array String))))

(defn- request->db-update-statement
  [lesson]
  (-> lesson
      (assoc :updated_on (instant/now))
      (update-if-exist :price bigdec)
      (update-if-exist :screenshots (partial into-array String))))

(defn add-lesson
  "Create new `lesson` in database with given `conn`."
  [conn lesson]
  (let [query (request->db-create-statement lesson)]
    (-> (sql/insert! conn :lessons query)
        :lessons/id)))

(defn update-lesson
  "Update existing `lesson` by `lesson-id` with given `conn`."
  [conn lesson-id lesson]
  (let [query (request->db-update-statement lesson)]
    (-> (sql/update! conn :lessons query {:id lesson-id})
        :next.jdbc/update-count)))

(defn get-lesson-by-id
  "Get existing lesson from database by `lesson-id` with given `conn`."
  [conn lesson-id]
  (sql/get-by-id conn :lessons lesson-id))

(defn get-all-lessons
  "Get all lessons from database."
  [conn & {:keys [limit offset]}]
  (let [query (-> (h/select :*)
                  (h/from :lessons)
                  (h/order-by [:created_on :asc])
                  (h/limit (or limit 20))
                  (h/offset (or offset 0))
                  (hsql/format))]
    (sql/query conn query)))

(defn delete-lesson
  "Delete lesson by `lesson-id` with given `conn`."
  [conn lesson-id]
  (-> (sql/delete! conn :lessons {:id lesson-id})
      :next.jdbc/update-count))
