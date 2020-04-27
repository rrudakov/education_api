(ns ring-learn.database.roles
  (:require [clojure.java.jdbc :as j]))

(defn get-all-roles
  "Fetch all roles from `database`."
  [database]
  (j/query (:connection database)
           ["SELECT * FROM roles"]))

(defn get-role-by-name
  "Get role from `database` by `name`."
  [database role-name]
  (j/get-by-id (:connection database) :roles role-name :role_name))
