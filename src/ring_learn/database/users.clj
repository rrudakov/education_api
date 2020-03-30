(ns ring-learn.database.users
  (:require [clojure.java.jdbc :as j]))

(defn add-user
  [database user]
  (let [{:keys [user_name user_password user_email is_admin]} user]
    (-> (j/insert! (:connection database) :users
                {:user_name user_name
                 :user_password user_password
                 :user_email user_email
                 :is_admin is_admin})
        (first)
        (:id))))

(defn get-user
  "Fetch user from database by ID."
  [database id]
  (j/query (:connection database)
           ["SELECT * FROM users WHERE id = ?" id]))

(defn get-all-users
  [database]
  (->> (j/query (:connection database)
                ["SELECT * FROM users"])
       (map (fn [u] (dissoc u :user_password)))))
