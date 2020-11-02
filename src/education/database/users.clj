(ns education.database.users
  (:require [buddy.hashers :as hs]
            [clojure.set :refer [rename-keys]]
            [education.database.roles :as roles]
            [education.http.constants :refer [invalid-credentials-error-message]]
            [honeysql.core :as hsql]
            [next.jdbc :as jdbc]
            [next.jdbc.sql :as sql]))

(defn add-user
  "Create new `user` in database."
  [conn user]
  (jdbc/with-transaction [tx conn]
    (let [{:keys [username password email]} user
          user_id (->> (sql/insert! tx :users
                                    {:user_name username
                                     :user_password (hs/encrypt password)
                                     :user_email email})
                       (:users/id))
          guest_role (roles/get-role-by-name tx "guest")]
      (sql/insert! tx :user_roles
                   {:user_id user_id
                    :role_id (:roles/id guest_role)})
      user_id)))

(defn- enrich-user-with-roles
  "Fetch roles for `user` and assoc `:roles` key with them."
  [conn user]
  (-> user
      (assoc :users/roles (roles/get-user-roles conn user))
      (dissoc :users/user_password)))

(defn get-user
  "Fetch user from database by `id`."
  [conn id]
  (some->> (sql/get-by-id conn :users id)
           (enrich-user-with-roles conn)))

(defn get-all-users
  "Fetch all users from `database`."
  [conn]
  (->> (hsql/build :select [:u.id
                            :u.user_name
                            :u.user_email
                            [:%array_agg.r.role_name :roles]
                            :u.created_on
                            :u.updated_on]
                   :from [[:users :u]]
                   :left-join [[:user_roles :ur] [:= :ur.user_id :u.id]
                               [:roles :r] [:= :r.id :ur.role_id]]
                   :group-by :u.id
                   :order-by [[:u.id :desc]])
       hsql/format
       (sql/query conn)
       (map #(rename-keys % {:roles :users/roles}))))

(defn update-user
  "Update `user` and `roles`."
  [conn id user]
  (let [available-roles (roles/get-all-roles conn)]
    (jdbc/with-transaction [tx conn]
      (let [{:keys [roles]} user
            new-roles (->> available-roles
                           (filter #(some #{(:roles/role_name %)} (map keyword roles)))
                           (map :roles/id))]
        (sql/delete! tx :user_roles {:user_id id})
        (sql/insert-multi! tx :user_roles
                           [:user_id :role_id]
                           (map #(vector id %) new-roles))
        (sql/update! tx :users {:updated_on (java.time.Instant/now)} {:id id})))))

(defn delete-user
  "Delete user from database by `user-id`."
  [conn user-id]
  (jdbc/with-transaction [tx conn]
    (sql/delete! tx :user_roles {:user_id user-id})
    (sql/delete! tx :users {:id user-id})))

(defn- get-user-by-username
  "Fetch user from `database` by `username`."
  [conn username]
  (sql/get-by-id conn :users username :user_name {}))

(defn auth-user
  "Check user credentials and authorize."
  [conn credentials]
  (let [user (get-user-by-username conn (:username credentials))
        noauth [false {:message invalid-credentials-error-message}]]
    (if user
      (if (hs/check (:password credentials) (:users/user_password user))
        [true {:user (enrich-user-with-roles conn user)}]
        noauth)
      noauth)))
