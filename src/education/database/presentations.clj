(ns education.database.presentations
  (:require [next.jdbc.sql :as sql]
            [honey.sql :as hsql]
            [honey.sql.helpers :as h]))

(defn add-presentation
  "Create new `presentation` entry in database with given `conn`."
  [conn {:keys [is_public] :as presentation :or {is_public true}}]
  (->> (assoc presentation :is_public is_public)
       (sql/insert! conn :presentations)
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
  [conn subtype-id & {:keys [limit offset]}]
  (->> (-> (h/select :*)
           (h/from :presentations)
           (h/where [:= :subtype_id subtype-id])
           (h/order-by [:updated_on :desc])
           (h/limit (or limit 20))
           (h/offset (or offset 0)))
       (hsql/format)
       (sql/query conn)
       (map (fn [presentation]
              (if (:presentations/is_public presentation)
                presentation
                (dissoc presentation :presentations/url))))))

(defn delete-presentation
  "Delete presentation by `presentation-id` with given `conn`."
  [conn presentation-id]
  (-> conn
      (sql/delete! :presentations {:id presentation-id})
      (:next.jdbc/update-count)))
