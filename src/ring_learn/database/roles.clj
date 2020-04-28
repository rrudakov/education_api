(ns ring-learn.database.roles
  (:require [clojure.java.jdbc :as j]
            [clojure.string :as str]))

(defn get-all-roles
  "Fetch all roles from `database`."
  [database]
  (j/query (:connection database)
           ["SELECT * FROM roles"]))

(defn get-role-by-name
  "Get role from `database` by `name`."
  [database role-name]
  (j/get-by-id (:connection database) :roles role-name :role_name))

(defn get-user-roles
  [database user]
  (let [user_id (:id user)
        conn (:connection database)]
    (->> ["SELECT r.role_name FROM user_roles AS ur LEFT JOIN roles AS r ON ur.role_id = r.id WHERE ur.user_id = ?" user_id]
         (j/query conn)
         (map :role_name)
         (map str/lower-case)
         (map keyword)
         (into #{}))))
