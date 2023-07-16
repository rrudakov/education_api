(ns education.database.presentations
  (:require
   [honey.sql :as hsql]
   [honey.sql.helpers :as h]
   [next.jdbc.sql :as sql]))

(defn add-presentation
  "Create new `presentation` entry in database with given `conn`."
  [conn {:keys [is_public] :as presentation :or {is_public true}}]
  (let [query (assoc presentation :is_public is_public)]
    (-> (sql/insert! conn :presentations query)
        :presentations/id)))

(defn update-presentation
  "Update existing `presentation` entry by `presentation-id` with given `conn`."
  [conn presentation-id presentation]
  (-> (sql/update! conn :presentations presentation {:id presentation-id})
      :next.jdbc/update-count))

(defn get-presentation-by-id
  "Get existing presentation from database by `presentation-id` with given `conn`."
  [conn presentation-id]
  (sql/get-by-id conn :presentations presentation-id))

(defn get-all-presentations
  "Get all presentations from database with given `conn`."
  [conn subtype-id & {:keys [limit offset]}]
  (let [query  (-> (h/select :*)
                   (h/from :presentations)
                   (h/where [:= :subtype_id subtype-id])
                   (h/order-by [:updated_on :desc])
                   (h/limit (or limit 20))
                   (h/offset (or offset 0))
                   (hsql/format))
        result (sql/query conn query)]
    (into []
          (map (fn [{:presentations/keys [is_public] :as presentation}]
                 (if is_public
                   presentation
                   (dissoc presentation :presentations/url))))
          result)))

(defn delete-presentation
  "Delete presentation by `presentation-id` with given `conn`."
  [conn presentation-id]
  (-> (sql/delete! conn :presentations {:id presentation-id})
      :next.jdbc/update-count))
