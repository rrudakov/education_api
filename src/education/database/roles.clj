(ns education.database.roles
  (:require [clojure.string :as str]
            [next.jdbc.sql :as sql]
            [next.jdbc.result-set :as rs]
            [honeysql.core :as hsql]))

(defn get-all-roles
  "Fetch all roles from database."
  [conn]
  (->> (hsql/build :select :* :from :roles)
       hsql/format
       (sql/query conn)
       (map #(update-in % [:roles/role_name] keyword))))

(defn get-role-by-name
  "Get role from database by `name`."
  [conn role-name]
  (sql/get-by-id conn :roles role-name :role_name {}))

(defn get-user-roles
  "Get roles assigned to particular `user`."
  [conn user]
  (->> (hsql/build :select [:r.role_name]
                   :from [[:user_roles :ur]]
                   :left-join [[:roles :r] [:= :ur.role_id :r.id]]
                   :where [:= :ur.user_id (:users/id user)])
       hsql/format
       (sql/query conn)
       (map :roles/role_name)
       (map str/lower-case)
       (map keyword)
       (into #{})))
