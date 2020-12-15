(ns education.database.gymnastics
  (:require [next.jdbc.sql :as sql]
            [honeysql.core :as hsql]))

(defn add-gymnastic
  "Create new `gymnastic` entry in database with given `conn`."
  [conn gymnastic]
  (-> conn
      (sql/insert! :gymnastics gymnastic)
      (:gymnastics/id)))

(defn update-gymnastic
  "Update existing `gymnastic` entry by `gymnastic-id` with given `conn`."
  [conn gymnastic-id gymnastic]
  (->> {:id gymnastic-id}
       (sql/update! conn :gymnastics gymnastic)
       (:next.jdbc/update-count)))

(defn get-gymnastic-by-id
  "Get existing gymnastic from database by `gymnastic-id` with given `conn`."
  [conn gymnastic-id]
  (sql/get-by-id conn :gymnastics gymnastic-id))

(defn get-all-gymnastics
  "Get all gymnastics from database with given `conn`.

  Accept optional parameters `limit` and `offset` to support pagination."
  [conn & {:keys [limit offset]}]
  (->> (hsql/build :select :*
                   :from :gymnastics
                   :order-by [[:updated_on :desc]]
                   :limit (or limit 20)
                   :offset (or offset 0))
       (hsql/format)
       (sql/query conn)))

(defn delete-gymnastic
  "Delete gymnastic by `gymnastic-id` with given `conn`."
  [conn gymnastic-id]
  (->> (sql/delete! conn :gymnastics {:id gymnastic-id})
       (:next.jdbc/update-count)))
