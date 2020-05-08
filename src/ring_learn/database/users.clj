(ns ring-learn.database.users
  (:require [buddy.hashers :as hs]
            [buddy.sign.jwt :as jwt]
            [clj-time.core :as t]
            [next.jdbc :as jdbc]
            [next.jdbc.sql :as sql]
            [ring-learn.config :as config]
            [ring-learn.database.roles :as roles]
            [next.jdbc.result-set :as rs]))

(defn add-user
  "Create new `user` in `database`."
  [database user]
  (jdbc/with-transaction [conn (:datasource database)]
    (let [{:keys [username password email]} user
          user_id (->> (sql/insert! conn :users
                                    {:user_name username
                                     :user_password (hs/encrypt password)
                                     :user_email email})
                       (:users/id))
          guest_role (roles/get-role-by-name database "guest")]
      (sql/insert! conn :user_roles
                   {:user_id user_id
                    :role_id (:roles/id guest_role)})
      user_id)))

(defn- enrich-user-with-roles
  "Fetch roles for `user` and assoc `:roles` key with them."
  [database user]
  (->> user
       (roles/get-user-roles database)
       (assoc user :users/roles)))

(defn get-user
  "Fetch user from `database` by `id`."
  [database id]
  (let [conn (:datasource database)
        user (sql/get-by-id conn :users id {})]
    (if user
      (enrich-user-with-roles database user)
      nil)))

(defn get-all-users
  "Fetch all users from `database`."
  [database]
  (->> {}
       (sql/query (:datasource database) ["SELECT * FROM users"])
       (map (partial enrich-user-with-roles database))))

(defn assign-roles
  "Assign or update `roles` for particular user."
  [database user-id roles]
  ())

(defn- get-user-by-username
  "Fetch user from `database` by `username`."
  [database username]
  (sql/get-by-id (:datasource database) :users username :user_name {}))

(defn auth-user
  "Check user credentials and authorize."
  [database credentials]
  (let [user (get-user-by-username database (:username credentials))
        noauth [false {:message "Invalid username or password"}]]
    (if user
      (if (hs/check (:password credentials) (:users/user_password user))
        [true {:user (enrich-user-with-roles database user)}]
        noauth)
      noauth)))
