(ns education.http.routes.users
  (:require
   [education.http.constants :as const]
   [education.http.endpoints.users :as users]
   [education.http.middleware :as auth]
   [education.specs.common :as specs-common]
   [education.specs.users :as specs-users]))

(defn auth-routes
  []
  ["/login"
   {:swagger {:tags ["users"]}
    :post    {:summary    "Authorize user using provided credentials"
              :parameters {:body ::specs-users/login-request}
              :responses  {200 {:description "Successful"
                                :body        ::specs-users/token-response}
                           401 {:description const/no-access-error-message
                                :body        ::specs-common/error-response}
                           403 {:description const/not-authorized-error-message
                                :body        ::specs-common/error-response}}
              :handler    users/login-handler}}])

(defn users-routes
  []
  ["/users"
   {:swagger {:tags ["users"]}}
   [""
    {:get  {:swagger    {:security [{:api_key []}]}
            :summary    "Get all users"
            :responses  {200 {:description "Successful"
                              :body        ::specs-users/users-response}
                         401 {:description const/no-access-error-message
                              :body        ::specs-common/error-response}
                         403 {:description const/not-authorized-error-message
                              :body        ::specs-common/error-response}}
            :middleware [[auth/require-roles #{:moderator}]]
            :handler    users/all-users-handler}
     :post {:summary    "Register new user"
            :parameters {:body ::specs-users/user-create-request}
            :responses  {200 {:description "Successful"
                              :body        ::specs-users/user-create-response}
                         400 {:description const/bad-request-error-message
                              :body        ::specs-common/error-response}
                         409 {:description const/conflict-error-message
                              :body        ::specs-common/error-response}}
            :handler    users/add-user-handler}}]
   ["/:user-id"
    {:get    {:swagger     {:security [{:api_key []}]}
              :summary     "Fetch user"
              :description "Fetch user from database by `:user-id`"
              :parameters  {:path {:user-id ::specs-users/id}}
              :middleware  [[auth/require-roles #{:moderator}]]
              :responses   {200 {:description "Successful"
                                 :body        ::specs-users/user-response}
                            401 {:description const/no-access-error-message
                                 :body        ::specs-common/error-response}
                            404 {:description const/not-found-error-message
                                 :body        ::specs-common/error-response}}
              :handler     users/get-user-handler}
     :patch  {:swagger     {:security [{:api_key []}]}
              :summary     "Update user"
              :description "Update user roles"
              :parameters  {:body ::specs-users/user-update-request
                            :path {:user-id ::specs-users/id}}
              :middleware  [[auth/require-roles #{:admin}]]
              :responses   {204 {:description "Successful"}
                            401 {:description const/no-access-error-message
                                 :body        ::specs-common/error-response}}
              :handler     users/update-user-handler}
     :delete {:swagger     {:security [{:api_key []}]}
              :summary     "Delete user"
              :description "Delete existing user by given `:user-id`"
              :parameters  {:path {:user-id ::specs-users/id}}
              :middleware  [[auth/require-roles #{:admin}]]
              :responses   {204 {:description "Successful"}
                            401 {:description const/no-access-error-message
                                 :body        ::specs-common/error-response}}
              :handler     users/delete-user-handler}}]])
