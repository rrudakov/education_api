(ns education.database.gymnastics
  (:require
   [next.jdbc.sql :as sql]
   [honey.sql :as hsql]
   [honey.sql.helpers :as h]))

(defn add-gymnastic
  "Create new `gymnastic` entry in database with given `conn`."
  [conn gymnastic]
  (-> (sql/insert! conn :gymnastics gymnastic)
      :gymnastics/id))

(defn update-gymnastic
  "Update existing `gymnastic` entry by `gymnastic-id` with given `conn`."
  [conn gymnastic-id gymnastic]
  (-> (sql/update! conn :gymnastics gymnastic {:id gymnastic-id})
      :next.jdbc/update-count))

(defn get-gymnastic-by-id
  "Get existing gymnastic from database by `gymnastic-id` with given `conn`."
  [conn gymnastic-id]
  (sql/get-by-id conn :gymnastics gymnastic-id))

(defn get-all-gymnastics
  "Get all gymnastics from database with given `conn`.

  Accept optional parameters `limit` and `offset` to support pagination."
  [conn subtype-id & {:keys [limit offset]}]
  (let [query (-> (h/select :*)
                  (h/from :gymnastics)
                  (h/where [:= :subtype_id subtype-id])
                  (h/order-by [:updated_on :desc])
                  (h/limit (or limit 20))
                  (h/offset (or offset 0))
                  (hsql/format))]
    (sql/query conn query)))

(defn delete-gymnastic
  "Delete gymnastic by `gymnastic-id` with given `conn`."
  [conn gymnastic-id]
  (-> (sql/delete! conn :gymnastics {:id gymnastic-id})
      :next.jdbc/update-count))
