(ns education.database.users
  (:require [buddy.hashers :as hs]
            [buddy.sign.jwt :as jwt]
            [clj-time.core :as t]
            [clojure.set :as set]
            [education.config :as config]
            [education.database.roles :as roles]
            [honeysql.core :as hsql]
            [next.jdbc :as jdbc]
            next.jdbc.date-time
            [next.jdbc.result-set :as rs]
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
  (->> user
       (roles/get-user-roles conn)
       (assoc user :users/roles)))

(defn get-user
  "Fetch user from database by `id`."
  [conn id]
  (some->> (sql/get-by-id conn :users id)
           (enrich-user-with-roles conn)))

(defn get-all-users
  "Fetch all users from `database`."
  [conn]
  (->> (hsql/build :select :* :from :users)
       hsql/format
       (sql/query conn)
       (map (partial enrich-user-with-roles conn))))

(defn update-user
  "Update `user` and `roles`."
  [conn id user]
  (let [available-roles (roles/get-all-roles conn)]
    (jdbc/with-transaction [tx conn]
      (let [{:keys [roles]} user
            new-roles (->> available-roles
                           (filter #(some #{(:roles/role_name %)} roles))
                           (map :roles/id))]
        (sql/delete! tx :user_roles {:user_id id})
        (sql/insert-multi! tx :user_roles
                           [:user_id :role_id]
                           (for [role new-roles]
                             [id role]))
        (sql/update! tx :users {:updated_on (java.time.Instant/now)} {:id id})))))

(defn delete-user
  "Delete user from database by `user-id`."
  [conn user-id]
  (j/with-transaction [tx conn]
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
        noauth [false {:message "Invalid username or password"}]]
    (if user
      (if (hs/check (:password credentials) (:users/user_password user))
        [true {:user (enrich-user-with-roles conn user)}]
        noauth)
      noauth)))