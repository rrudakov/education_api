(ns education.http.routes.dresses
  (:require
   [education.http.constants :as const]
   [education.http.endpoints.dresses :as dresses]
   [education.http.middleware :as auth]
   [education.specs.common :as specs-common]))

(defn dresses-routes
  []
  ["/dresses"
   {:swagger {:tags ["dresses"]}}
   [""
    {:post {:swagger    {:security [{:api_key []}]}
            :summary    "Create new dress"
            :middleware [[auth/require-roles #{:admin}]]
            :parameters {:body ::specs-common/dress-create-request}
            :responses  {201 {:description "Dress created successfully"
                              :body        ::specs-common/create-response}
                         400 {:description const/bad-request-error-message
                              :body        ::specs-common/error-response}
                         401 {:description const/not-authorized-error-message
                              :body        ::specs-common/error-response}
                         403 {:description const/no-access-error-message
                              :body        ::specs-common/error-response}}
            :handler    dresses/create-dress-handler}
     :get  {:summary     "Get all dresses"
            :description "Get list of dresses with given optional `:limit` and `:offset`"
            :parameters  {:query ::specs-common/optional-limit-offset}
            :responses   {200 {:description "Successful response"
                               :body        ::specs-common/dresses-response}
                          400 {:description const/bad-request-error-message
                               :body        ::specs-common/error-response}}
            :handler     dresses/get-all-dresses-handler}}]
   ["/:dress-id"
    {:get    {:summary     "Get dress"
              :description "Get dress by `:dress-id`"
              :parameters  {:path {:dress-id ::specs-common/id}}
              :responses   {200 {:description "Dress was successfully found in the database"
                                 :body        ::specs-common/dress-response}
                            400 {:description const/bad-request-error-message
                                 :body        ::specs-common/error-response}
                            404 {:description const/not-found-error-message
                                 :body        ::specs-common/error-response}}
              :handler     dresses/get-dress-by-id-handler}
     :patch  {:swagger     {:security [{:api_key []}]}
              :summary     "Update dress"
              :description "Update dress by `:dress-id`"
              :parameters  {:path {:dress-id ::specs-common/id}
                            :body ::specs-common/dress-update-request}
              :middleware  [[auth/require-roles #{:admin}]]
              :responses   {204 {:description "Successful"}
                            400 {:description const/bad-request-error-message
                                 :body        ::specs-common/error-response}
                            401 {:description const/not-authorized-error-message
                                 :body        ::specs-common/error-response}
                            403 {:description const/no-access-error-message
                                 :body        ::specs-common/error-response}
                            404 {:description const/not-found-error-message
                                 :body        ::specs-common/error-response}}
              :handler     dresses/update-dress-handler}
     :delete {:swagger     {:security [{:api_key []}]}
              :summary     "Delete dress"
              :description "Delete dress by `:dress-id`"
              :parameters  {:path {:dress-id ::specs-common/id}}
              :middleware  [[auth/require-roles #{:admin}]]
              :responses   {204 {:description "Successful"}
                            400 {:description const/bad-request-error-message
                                 :body        ::specs-common/error-response}
                            401 {:description const/not-authorized-error-message
                                 :body        ::specs-common/error-response}
                            403 {:description const/no-access-error-message
                                 :body        ::specs-common/error-response}
                            404 {:description const/not-found-error-message
                                 :body        ::specs-common/error-response}}
              :handler     dresses/delete-dress-by-id-handler}}]])
