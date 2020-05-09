(ns ring-learn.database.roles
  (:require [clojure.string :as str]
            [next.jdbc.sql :as sql]
            [next.jdbc.result-set :as rs]))

(defn get-all-roles
  "Fetch all roles from `database`."
  [database]
  (->> ["SELECT * FROM roles"]
       (sql/query (:datasource database))
       (map #(update-in % [:roles/role_name] keyword))))

(defn get-role-by-name
  "Get role from `database` by `name`."
  [database role-name]
  (sql/get-by-id (:datasource database) :roles role-name :role_name {}))

(defn get-user-roles
  "Get roles assigned to particular `user`."
  [database user]
  (let [user_id (:users/id user)
        conn (:datasource database)]
    (->> {}
         (sql/query conn ["SELECT r.role_name FROM user_roles AS ur LEFT JOIN roles AS r ON ur.role_id = r.id WHERE ur.user_id = ?" user_id])
         (map :roles/role_name)
         (map str/lower-case)
         (map keyword)
         (into #{}))))
