(ns education.http.endpoints.roles
  (:require
   [education.database.roles :as rolesdb]
   [ring.util.http-response :refer [ok]]))

(defn- to-roles-response
  "Convert database roles to roles response."
  [{:roles/keys [id role_name]}]
  {:id   id
   :name (keyword role_name)})

(defn roles-handler
  "Return all available roles."
  [{:keys [conn]}]
  (->> (rolesdb/get-all-roles conn)
       (into #{} (map to-roles-response))
       ok))
