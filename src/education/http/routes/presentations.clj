(ns education.http.routes.presentations
  (:require
   [education.http.constants :as const]
   [education.http.endpoints.presentations :as presentations]
   [education.http.middleware :as auth]
   [education.specs.common :as specs-common]))

(defn presentations-routes
  []
  ["/presentations"
   {:swagger {:tags ["presentations"]}}
   [""
    {:get  {:summary     "Get all presentations"
            :description "Get list of presentations by `subtype_id` with given `limit` and `offset`"
            :parameters  {:query ::specs-common/presentations-all-query-parameters}
            :responses   {200 {:description "Successful response"
                               :body        ::specs-common/presentations-response}
                          400 {:description const/bad-request-error-message
                               :schema      ::specs-common/error-response}}
            :handler     presentations/get-all-presentations-handler}
     :post {:swagger    {:security [{:api_key []}]}
            :summary    "Create new presentation"
            :middleware [[auth/require-roles #{:admin}]]
            :parameters {:body ::specs-common/presentation-create-request}
            :responses  {201 {:description "Presentation created successfully"
                              :body        ::specs-common/create-response}
                         400 {:description const/bad-request-error-message
                              :body        ::specs-common/error-response}
                         401 {:description const/not-authorized-error-message
                              :body        ::specs-common/error-response}
                         403 {:description const/no-access-error-message
                              :body        ::specs-common/error-response}}
            :handler    presentations/create-presentation-handler}}]
   ["/:presentation-id"
    {:get    {:swagger     {:security [{:api_key []}]}
              :summary     "Get presentation entry"
              :description "Get presentation by `presentation-id`"
              :middleware  [[auth/require-roles #{:admin}]]
              :parameters  {:path {:presentation-id ::specs-common/id}}
              :responses   {200 {:description "Presentation was successfully found in the database"
                                 :body        ::specs-common/presentation-response}
                            400 {:description const/bad-request-error-message
                                 :body        ::specs-common/error-response}
                            404 {:description const/not-found-error-message
                                 :body        ::specs-common/error-response}}
              :handler     presentations/get-presentation-by-id-handler}
     :patch  {:swagger     {:security [{:api_key []}]}
              :summary     "Update presentation entry"
              :description "Update existing presentation by `presentation-id`"
              :middleware  [[auth/require-roles #{:admin}]]
              :parameters  {:path {:presentation-id ::specs-common/id}
                            :body ::specs-common/presentation-update-request}
              :responses   {204 {:description "Presentation updated successfully"}
                            400 {:description const/bad-request-error-message
                                 :body        ::specs-common/error-response}
                            401 {:description const/not-authorized-error-message
                                 :body        ::specs-common/error-response}
                            403 {:description const/no-access-error-message
                                 :body        ::specs-common/error-response}
                            404 {:description const/not-found-error-message
                                 :body        ::specs-common/error-response}}
              :handler     presentations/update-presesntation-handler}
     :delete {:swagger     {:security [{:api_key []}]}
              :summary     "Delete presentation entry"
              :description "Delete presentation by `presentation-id`"
              :middleware  [[auth/require-roles #{:admin}]]
              :parameters  {:path {:presentation-id ::specs-common/id}}
              :responses   {204 {:description "Presentation deleted successfully"}
                            400 {:description const/bad-request-error-message
                                 :body        ::specs-common/error-response}
                            401 {:description const/not-authorized-error-message
                                 :body        ::specs-common/error-response}
                            403 {:description const/no-access-error-message
                                 :body        ::specs-common/error-response}
                            404 {:description const/not-found-error-message
                                 :body        ::specs-common/error-response}}
              :handler     presentations/delete-presentation-by-id-handler}}]])
