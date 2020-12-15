(ns education.database.dresses
  (:require [education.utils.maps :refer [update-if-exist]]
            [next.jdbc.sql :as sql]
            [honeysql.core :as hsql])
  (:import java.time.Instant))

(defn- request->db-create-statement
  [dress]
  (-> dress
      (update :price bigdec)
      (update :pictures (partial into-array String))))

(defn- request->db-update-statement
  [dress]
  (-> dress
      (assoc :updated_on (Instant/now))
      (update-if-exist :price bigdec)
      (update-if-exist :pictures (partial into-array String))))

(defn add-dress
  "Create new `dress` entry in database with given `conn`."
  [conn dress]
  (->> dress
       (request->db-create-statement)
       (sql/insert! conn :dresses)
       (:dresses/id)))

(defn update-dress
  "Update existing `dress` entry by `dress-id` with given `conn`."
  [conn dress-id dress]
  (->> {:id dress-id}
       (sql/update! conn :dresses (request->db-update-statement dress))
       (:next.jdbc/update-count)))

(defn get-dress-by-id
  "Get existing dress from database by `dress-id` with given `conn`."
  [conn dress-id]
  (sql/get-by-id conn :dresses dress-id))

(defn get-all-dresses
  "Get all dresses from database with given `conn`.

  Accept optional parameters `limit` and `offset` to support pagination."
  [conn & {:keys [limit offset]}]
  (->> (hsql/build :select :*
                   :from :dresses
                   :order-by [[:updated_on :desc]]
                   :limit (or limit 20)
                   :offset (or offset 0))
       (hsql/format)
       (sql/query conn)))

(defn delete-dress
  "Delete dress by `dress-id` with given `conn`."
  [conn dress-id]
  (->> (sql/delete! conn :dresses {:id dress-id})
       (:next.jdbc/update-count)))
