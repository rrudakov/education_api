(ns education.http.endpoints.roles
  (:require [compojure.api.sweet :refer [context GET]]
            [education.database.roles :as rolesdb]
            [education.http.restructure :refer [require-roles]]
            [education.specs.roles :as specs]
            [ring.util.http-response :refer [ok]]))

(defn- to-roles-response
  "Convert database roles to roles response."
  [{:roles/keys [id role_name]}]
  {:id id
   :name (keyword role_name)})

(defn roles-handler
  "Return all available roles."
  [db]
  (->> db
       rolesdb/get-all-roles
       (map to-roles-response)
       (into #{})
       ok))

(defn roles-routes
  "Define routes for roles endpoint."
  [db]
  (context "" []
    :tags ["roles"]
    (GET "/roles" []
        :middleware [[require-roles #{:admin}]]
        :return ::specs/roles-response
        :summary "Return list of all available roles"
        (roles-handler db))))
