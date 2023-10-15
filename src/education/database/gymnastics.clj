(ns education.database.gymnastics
  (:require
   [next.jdbc.optional :as jdbc.optional]
   [next.jdbc.sql :as sql]))

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
  (sql/get-by-id conn
                 :gymnastics
                 gymnastic-id
                 {:builder-fn jdbc.optional/as-maps}))

(defn get-all-gymnastics
  "Get all gymnastics from database with given `conn`.

  Accept optional parameters `limit` and `offset` to support pagination."
  [conn subtype-id & {:keys [limit offset]}]
  (sql/find-by-keys conn
                    :gymnastics
                    {:subtype_id subtype-id}
                    {:builder-fn jdbc.optional/as-maps
                     :limit      (or limit 20)
                     :offset     (or offset 0)
                     :order-by   [[:updated_on :desc]]}))

(defn delete-gymnastic
  "Delete gymnastic by `gymnastic-id` with given `conn`."
  [conn gymnastic-id]
  (-> (sql/delete! conn :gymnastics {:id gymnastic-id})
      :next.jdbc/update-count))
