(ns ring-learn.database.users
  (:require [buddy.hashers :as hs]
            [buddy.sign.jwt :as jwt]
            [clj-time.core :as t]
            [clojure.java.jdbc :as j]
            [ring-learn.config :as config]
            [ring-learn.database.roles :as roles]))

(defn add-user
  "Create new `user` in `database`."
  [database user]
  (j/with-db-transaction [conn (:connection database)]
    (let [{:keys [user_name user_password user_email]} user
          user_id (-> (j/insert! conn :users
                                 {:user_name user_name
                                  :user_password (hs/encrypt user_password)
                                  :user_email user_email})
                      (first)
                      (:id))
          guest_role (roles/get-role-by-name database "guest")]
      (j/insert! conn :user_roles
                 {:user_id user_id
                  :role_id (:id guest_role)})
      user_id)))

(defn- enrich-user-with-roles
  "Fetch roles for `user` and assoc `:roles` key with them."
  [database user]
  (-> user
      (assoc :roles (roles/get-user-roles database user))
      (dissoc :user_password)))

(defn get-user
  "Fetch user from `database` by `id`."
  [database id]
  (let [conn (:connection database)
        user (j/get-by-id conn :users id)]
    (enrich-user-with-roles database user)))

(defn get-all-users
  "Fetch all users from `database`."
  [database]
  (->> (j/query (:connection database) ["SELECT * FROM users"])
       (map (partial enrich-user-with-roles database))))

(defn- get-user-by-username
  "Fetch user from `database` by `username`."
  [database username]
  (j/get-by-id (:connection database) :users username :user_name))

(defn auth-user
  "Check user credentials and authorize."
  [database credentials]
  (let [user (get-user-by-username database (:username credentials))
        noauth [false {:message "Invalid username or password"}]]
    (if user
      (if (hs/check (:password credentials) (:user_password user))
        [true {:user (enrich-user-with-roles database user)}]
        noauth)
      noauth)))

(defn create-auth-token
  "Create authorization token based on `credentials`."
  [database config credentials]
  (let [[ok? res] (auth-user database credentials)
        exp (t/plus (t/now) (t/days 1))]
    (if ok?
      [true {:token (jwt/sign (assoc res :exp exp)
                              (config/token-sign-secret config)
                              {:alg :hs512})}]
      [false res])))
