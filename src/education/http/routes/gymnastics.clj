(ns education.http.routes.gymnastics
  (:require
   [education.http.constants :as const]
   [education.http.endpoints.gymnastics :as gymnastics]
   [education.http.middleware :as auth]
   [education.specs.common :as specs-common]))

(defn gymnastics-routes
  []
  ["/gymnastics"
   {:swagger {:tags ["gymnastics"]}}
   [""
    {:get  {:summary     "Get all gymnastics"
            :description "Get list of gymnastics with given optional `limit` and `offset`"
            :parameters  {:query ::specs-common/gymnastics-all-query-params}
            :responses   {200 {:description "Successful"
                               :body        ::specs-common/gymnastics-response}
                          400 {:description const/bad-request-error-message
                               :body        ::specs-common/error-response}}
            :handler     gymnastics/get-all-gymnastics-handler}
     :post {:swagger    {:security [{:api_key []}]}
            :summary    "Create new gymnastic"
            :middleware [[auth/require-roles #{:admin}]]
            :parameters {:body ::specs-common/gymnastic-create-request}
            :responses  {201 {:description "Gymnastic created successfully"
                              :body        ::specs-common/create-response}
                         400 {:description const/bad-request-error-message
                              :body        ::specs-common/error-response}
                         401 {:description const/not-authorized-error-message
                              :body        ::specs-common/error-response}
                         403 {:description const/no-access-error-message
                              :body        ::specs-common/error-response}}
            :handler    gymnastics/create-gymnastic-handler}}]
   ["/:gymnastic-id"
    {:get    {:summary     "Get gymnastic entry"
              :description "Get gymnastic by `gymnastic-id`"
              :parameters  {:path {:gymnastic-id ::specs-common/id}}
              :responses   {200 {:description "Gymnastic was successfully found in the database"
                                 :body        ::specs-common/gymnastic-response}
                            400 {:description const/bad-request-error-message
                                 :body        ::specs-common/error-response}
                            404 {:description const/not-found-error-message
                                 :body        ::specs-common/error-response}}
              :handler     gymnastics/get-gymnastic-by-id-handler}
     :patch  {:swagger     {:security [{:api_key []}]}
              :summary     "Update gymnastic entry"
              :description "Update existing gymnastic by `gymnastic-id`"
              :middleware  [[auth/require-roles #{:admin}]]
              :parameters  {:path {:gymnastic-id ::specs-common/id}
                            :body ::specs-common/gymnastic-update-request}
              :responses   {204 {:description "Successful"}
                            400 {:description const/bad-request-error-message
                                 :body        ::specs-common/error-response}
                            401 {:description const/not-authorized-error-message
                                 :body        ::specs-common/error-response}
                            403 {:description const/no-access-error-message
                                 :body        ::specs-common/error-response}
                            404 {:description const/not-found-error-message
                                 :body        ::specs-common/error-response}}
              :handler     gymnastics/update-gymnastic-handler}
     :delete {:swagger     {:security [{:api_key []}]}
              :summary     "Delete gymnastic entry"
              :description "Delete gymnastic entry by `gymnastic-id`"
              :middleware  [[auth/require-roles #{:admin}]]
              :parameters  {:path {:gymnastic-id ::specs-common/id}}
              :responses   {204 {:description "Successful"}
                            400 {:description const/bad-request-error-message
                                 :body        ::specs-common/error-response}
                            401 {:description const/not-authorized-error-message
                                 :body        ::specs-common/error-response}
                            403 {:description const/no-access-error-message
                                 :body        ::specs-common/error-response}
                            404 {:description const/not-found-error-message
                                 :body        ::specs-common/error-response}}
              :handler     gymnastics/delete-gymnastic-by-id-handler}}]])
