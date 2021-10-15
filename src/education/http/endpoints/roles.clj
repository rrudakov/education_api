(ns education.http.endpoints.roles
  (:require
   [education.database.roles :as rolesdb]
   [ring.util.http-response :as status]))

;;; Helpers

(defn- to-roles-response
  "Convert database roles to roles response."
  [{:roles/keys [id role_name]}]
  {:id   id
   :name (keyword role_name)})

;;; Handlers

(defn roles-handler
  "Return all available roles."
  [{:keys [conn]}]
  (->> (rolesdb/get-all-roles conn)
       (into #{} (map to-roles-response))
       status/ok))
