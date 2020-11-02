(ns education.http.endpoints.roles
  (:require [compojure.api.sweet :refer [context GET]]
            [education.database.roles :as rolesdb]
            [education.http.restructure :refer [require-roles]]
            [education.specs.roles :as specs]
            [ring.util.http-response :refer [ok]]
            [education.specs.error :as err]
            [education.http.constants :as const]))

(defn- to-roles-response
  "Convert database roles to roles response."
  [{:roles/keys [id role_name]}]
  {:id   id
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
  (context "/roles" []
    (GET "/" []
      :tags ["roles"]
      :middleware [[require-roles #{:admin}]]
      :summary "Return list of all available roles"
      :responses {200 {:description "Successful"
                       :schema      ::specs/roles-response}
                  401 {:description const/no-access-error-message
                       :schema      ::err/error-response}
                  403 {:description const/not-authorized-error-message
                       :schema      ::err/error-response}}
      (roles-handler db))))
