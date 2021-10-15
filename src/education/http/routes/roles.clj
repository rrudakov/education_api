(ns education.http.routes.roles
  (:require
   [education.http.constants :as const]
   [education.http.endpoints.roles :as roles]
   [education.http.middleware :as auth]
   [education.specs.common :as specs-common]
   [education.specs.roles :as specs-roles]))

(defn roles-routes
  []
  ["/roles"
   {:swagger {:tags     ["roles"]
              :security [{:api_key []}]}
    :get     {:summary    "Return list of all available roles"
              :middleware [[auth/require-roles #{:admin}]]
              :responses  {200 {:description "Successful"
                                :body        ::specs-roles/roles-response}
                           401 {:description const/no-access-error-message
                                :body        ::specs-common/error-response}
                           403 {:description const/not-authorized-error-message
                                :body        ::specs-common/error-response}}
              :handler    roles/roles-handler}}])
