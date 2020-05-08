(ns ring-learn.http.endpoints.roles
  (:require [compojure.api.sweet :refer [context GET]]
            [ring-learn.database.roles :as rolesdb]
            [ring-learn.http.restructure :refer [require-roles]]
            [ring.util.http-response :refer [ok]]
            [schema.core :as s]))

(s/defschema Role
  {:id s/Int
   :name s/Keyword})

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
        :return #{Role}
        :summary "Return list of all available roles"
        (roles-handler db))))
