(ns education.database.articles
  (:require [education.http.restructure :refer [has-role?]]
            [honeysql.core :as hsql]
            next.jdbc.date-time
            [next.jdbc.sql :as sql]))

(def article-owerview-sql-map
  "Base sqlmap for selecting many articles."
  (hsql/build :select [:id
                       :user_id
                       :title
                       :featured_image
                       :updated_on
                       :description]
              :from :articles))

(def article-full-sql-map
  "Base sqlmap for selection full sized article."
  (hsql/build :select [:id
                       :user_id
                       :title
                       :body
                       :featured_image
                       :created_on
                       :updated_on
                       :description]
              :from :articles))

(def default-article-description
  "If description in request is omitted use this one."
  "No description...")

(defn add-article
  "Create new `article` in database."
  [conn user article]
  (let [{:keys [title body featured_image is_main_featured description]
         :or {is_main_featured false
              description default-article-description}} article
        user-id (:id user)]
    (->> (sql/insert! conn :articles
                      {:user_id          user-id
                       :title            title
                       :body             body
                       :featured_image   featured_image
                       :description      description
                       :is_main_featured is_main_featured})
         (:articles/id))))

(defn update-article
  "Update existing `article`."
  [conn article-id article]
  (let [{:keys [title body featured_image is_main_featured description]} article
        values {:title title
                :body body
                :featured_image featured_image
                :is_main_featured is_main_featured
                :description description}
        values-for-update (assoc (apply dissoc values (for [[k v] values :when (nil? v)] k))
                                 :updated_on (java.time.Instant/now))]
    (:next.jdbc/update-count (sql/update! conn :articles values-for-update {:id article-id}))))

(defn get-all-articles
  "Fetch recent articles from database."
  ([conn]
   (get-all-articles conn 100))
  ([conn limit]
   (->> limit
        (hsql/build article-owerview-sql-map :order-by [[:updated_on :desc]] :limit)
        hsql/format
        (sql/query conn))))

(defn get-latest-full-sized-articles
  "Fetch `number` of latest articles with all fields."
  [conn number]
  (->> (hsql/build article-full-sql-map
                   :order-by [[:updated_on :desc]]
                   :limit number)
       hsql/format
       (sql/query conn)))

(defn get-user-articles
  "Fetch recent articles from database for particular `user`."
  ([conn user-id] (get-user-articles conn user-id 100))
  ([conn user-id limit]
   (->> (hsql/build article-owerview-sql-map
                    :where [:= :user_id user-id]
                    :order-by [[:updated_on :desc]]
                    :limit limit)
        hsql/format
        (sql/query conn))))

(defn get-article-by-id
  "Fetch single article from database by `article-id`."
  [conn article-id]
  (sql/get-by-id conn :articles article-id))

(defn get-last-featured-article
  [conn]
  (let [result (->> (hsql/build article-owerview-sql-map
                                :where [:= :is_main_featured true]
                                :order-by [[:updated_on :desc]]
                                :limit 1)
                    hsql/format
                    (sql/query conn))]
    (if (seq result)
      (first result)
      nil)))

(defn get-last-featured-articles
  [conn limit]
  (->> (hsql/build article-owerview-sql-map
                   :where [:= :is_main_featured true]
                   :order-by [[:updated_on :desc]]
                   :limit limit
                   :offset 1)
       hsql/format
       (sql/query conn)))

(defn delete-article
  "Delete article from database by `article-id`."
  [conn article-id]
  (sql/delete! conn :articles {:id article-id}))

(defn can-update?
  "Check whether `user` allowed to update article by `arcicle-id`."
  [conn user article-id]
  (or (has-role? user #{:moderator})
      (let [article-user-id (:articles/user_id (sql/get-by-id conn :articles article-id))]
        (= (:id user) article-user-id))))
