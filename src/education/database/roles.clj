(ns education.database.roles
  (:require [clojure.string :as str]
            [honey.sql :as hsql]
            [next.jdbc.sql :as sql]
            [honey.sql.helpers :as h]))

(defn get-all-roles
  "Fetch all roles from database."
  [conn]
  (->> (-> (h/select :*)
           (h/from :roles))
       (hsql/format)
       (sql/query conn)
       (map #(update-in % [:roles/role_name] keyword))))

(defn get-role-by-name
  "Get role from database by `name`."
  [conn role-name]
  (sql/get-by-id conn :roles role-name :role_name {}))

(defn get-user-roles
  "Get roles assigned to particular `user`."
  [conn user]
  (->> (-> (h/select :r.role_name)
           (h/from [:user_roles :ur])
           (h/left-join [:roles :r] [:= :ur.role_id :r.id])
           (h/where [:= :ur.user_id (:users/id user)]))
       (hsql/format)
       (sql/query conn)
       (map :roles/role_name)
       (map str/lower-case)
       (map keyword)
       (into #{})))
