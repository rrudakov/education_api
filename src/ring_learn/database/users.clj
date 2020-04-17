(ns ring-learn.database.users
  (:require [buddy.hashers :as hs]
            [buddy.sign.jwt :as jwt]
            [clj-time.core :as t]
            [clojure.java.jdbc :as j]
            [ring-learn.config :as config]))

(defn add-user
  "Create new `user` in `database`."
  [database user]
  (let [{:keys [user_name user_password user_email is_admin]} user]
    (-> (j/insert! (:connection database) :users
                {:user_name user_name
                 :user_password (hs/encrypt user_password)
                 :user_email user_email
                 :is_admin is_admin})
        (first)
        (:id))))

(defn get-user
  "Fetch user from `database` by `id`."
  [database id]
  (-> (j/get-by-id (:connection database) :users id)
      (dissoc :user_password)))

(defn get-all-users
  "Fetch all users from `database`."
  [database]
  (->> (j/query (:connection database)
                ["SELECT * FROM users"])
       (map (fn [u] (dissoc u :user_password)))))

(defn get-user-by-username
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
        [true {:user (dissoc user :user_password)}]
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
