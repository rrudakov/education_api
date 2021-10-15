(ns education.http.routes.materials
  (:require
   [education.http.constants :as const]
   [education.http.endpoints.materials :as materials]
   [education.http.middleware :as auth]
   [education.specs.common :as specs-common]))

(defn materials-routes
  []
  ["/materials"
   {:swagger {:tags ["materials"]}}
   [""
    {:get  {:summary     "Get all materials"
            :description "Get list of materials with given `limit` and `offset`"
            :parameters  {:query ::specs-common/optional-limit-offset}
            :responses   {200 {:description "Successful response"
                               :body        ::specs-common/materials-response}
                          400 {:description const/bad-request-error-message
                               :body        ::specs-common/error-response}}
            :handler     materials/get-all-materials-handler}
     :post {:swagger    {:security [{:api_key []}]}
            :summary    "Create new material"
            :middleware [[auth/require-roles #{:admin}]]
            :parameters {:body ::specs-common/material-create-request}
            :responses  {201 {:description "Material created successfully"
                              :body        ::specs-common/create-response}
                         400 {:description const/bad-request-error-message
                              :body        ::specs-common/error-response}
                         401 {:description const/not-authorized-error-message
                              :body        ::specs-common/error-response}
                         403 {:description const/no-access-error-message
                              :body        ::specs-common/error-response}}
            :handler    materials/create-material-handler}}]
   ["/:material-id"
    {:get    {:summary     "Get material entry"
              :description "Get material by `material-id`"
              :parameters  {:path {:material-id ::specs-common/id}}
              :responses   {200 {:description "Material was successfully found in the database"
                                 :body        ::specs-common/material-response}
                            400 {:description const/bad-request-error-message
                                 :body        ::specs-common/error-response}
                            404 {:description const/not-found-error-message
                                 :body        ::specs-common/error-response}}
              :handler     materials/get-material-by-id-handler}
     :patch  {:swagger     {:security [{:api_key []}]}
              :summary     "Update material entry"
              :description "Update material by `material-id`"
              :middleware  [[auth/require-roles #{:admin}]]
              :parameters  {:path {:material-id ::specs-common/id}
                            :body ::specs-common/material-update-request}
              :responses   {204 {:description "Material was updated successfully"}
                            400 {:description const/bad-request-error-message
                                 :body        ::specs-common/error-response}
                            401 {:description const/not-authorized-error-message
                                 :body        ::specs-common/error-response}
                            403 {:description const/no-access-error-message
                                 :body        ::specs-common/error-response}
                            404 {:description const/not-found-error-message
                                 :body        ::specs-common/error-response}}
              :handler     materials/update-material-handler}
     :delete {:swagger     {:security [{:api_key []}]}
              :summary     "Delete material entry"
              :description "Delete material entry by `material-id`"
              :middleware  [[auth/require-roles #{:admin}]]
              :parameters  {:path {:material-id ::specs-common/id}}
              :responses   {204 {:description "Material was deleted successfully"}
                            400 {:description const/bad-request-error-message
                                 :body        ::specs-common/error-response}
                            401 {:description const/not-authorized-error-message
                                 :body        ::specs-common/error-response}
                            403 {:description const/no-access-error-message
                                 :body        ::specs-common/error-response}
                            404 {:description const/not-found-error-message
                                 :body        ::specs-common/error-response}}
              :handler     materials/delete-material-by-id-handler}}]])
