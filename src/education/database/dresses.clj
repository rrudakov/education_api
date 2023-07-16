(ns education.database.dresses
  (:require
   [cljc.java-time.instant :as instant]
   [education.utils.maps :refer [update-if-exist]]
   [honey.sql :as hsql]
   [honey.sql.helpers :as h]
   [next.jdbc.sql :as sql]))

(defn- request->db-create-statement
  [dress]
  (-> dress
      (update :price bigdec)
      (update :pictures (partial into-array String))))

(defn- request->db-update-statement
  [dress]
  (-> dress
      (assoc :updated_on (instant/now))
      (update-if-exist :price bigdec)
      (update-if-exist :pictures (partial into-array String))))

(defn add-dress
  "Create new `dress` entry in database with given `conn`."
  [conn dress]
  (let [query (request->db-create-statement dress)]
    (-> (sql/insert! conn :dresses query)
        :dresses/id)))

(defn update-dress
  "Update existing `dress` entry by `dress-id` with given `conn`."
  [conn dress-id dress]
  (let [query (request->db-update-statement dress)]
    (-> (sql/update! conn :dresses query {:id dress-id})
        :next.jdbc/update-count)))

(defn get-dress-by-id
  "Get existing dress from database by `dress-id` with given `conn`."
  [conn dress-id]
  (sql/get-by-id conn :dresses dress-id))

(defn get-all-dresses
  "Get all dresses from database with given `conn`.

  Accept optional parameters `limit` and `offset` to support pagination."
  [conn & {:keys [limit offset]}]
  (let [query (-> (h/select :*)
                  (h/from :dresses)
                  (h/order-by [:updated_on :desc])
                  (h/limit (or limit 20))
                  (h/offset (or offset 0))
                  (hsql/format))]
    (sql/query conn query)))

(defn delete-dress
  "Delete dress by `dress-id` with given `conn`."
  [conn dress-id]
  (-> (sql/delete! conn :dresses {:id dress-id})
      :next.jdbc/update-count))
