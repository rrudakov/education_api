(ns education.database.presentations
  (:require [next.jdbc.sql :as sql]
            [honeysql.core :as hsql]))

(defn add-presentation
  "Create new `presentation` entry in database with given `conn`."
  [conn presentation]
  (-> conn
      (sql/insert! :presentations presentation)
      (:presentations/id)))

(defn update-presentation
  "Update existing `presentation` entry by `presentation-id` with given `conn`."
  [conn presentation-id presentation]
  (-> conn
      (sql/update! :presentations presentation {:id presentation-id})
      (:next.jdbc/update-count)))

(defn get-presentation-by-id
  "Get existing presentation from database by `presentation-id` with given `conn`."
  [conn presentation-id]
  (sql/get-by-id conn :presentations presentation-id))

(defn get-all-presentations
  "Get all presentations from database with given `conn`."
  [conn & {:keys [limit offset]}]
  (->> (hsql/build :select :*
                   :from :presentations
                   :order-by [[:updated_on :desc]]
                   :limit (or limit 20)
                   :offset (or offset 0))
       (hsql/format)
       (sql/query conn)))

(defn delete-presentation
  "Delete presentation by `presentation-id` with given `conn`."
  [conn presentation-id]
  (-> conn
      (sql/delete! :presentations {:id presentation-id})
      (:next.jdbc/update-count)))
