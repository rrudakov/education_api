(ns education.http.routes
  (:require
   [buddy.auth.middleware :refer [wrap-authentication]]
   [clojure.spec.alpha :as s]
   [compojure.api.api :refer [api]]
   [compojure.api.core :refer [context]]
   [education.config :as config]
   [education.database.middleware :as db-mw]
   [education.error :as error]
   [education.http.constants :as const]
   [education.http.endpoints.dresses :as dresses]
   [education.http.endpoints.gymnastics :refer [gymnastics-routes]]
   [education.http.endpoints.lessons :as lessons]
   [education.http.endpoints.materials :refer [materials-routes]]
   [education.http.endpoints.presentations :refer [presentations-routes]]
   [education.http.endpoints.roles :as roles]
   [education.http.endpoints.upload :refer [upload-routes]]
   [education.http.endpoints.users :as users]
   [education.http.middleware :as auth]
   [education.specs.common :as specs-common]
   [education.specs.roles :as specs-roles]
   [education.specs.users :as specs-users]
   [muuntaja.core :as m]
   [reitit.coercion.spec :as rcs]
   [reitit.ring.coercion :as rrc]
   [reitit.ring.middleware.multipart :as multipart]
   [reitit.ring.middleware.muuntaja :as muuntaja]
   [reitit.ring.middleware.parameters :as parameters]
   [reitit.swagger :as swagger]
   [reitit.swagger-ui :as swagger-ui]
   [ring.middleware.cors :refer [wrap-cors]]
   [ring.util.http-response :as status]
   [taoensso.timbre :refer [error trace]]))

(defn response-validation-handler
  "Return error in case of invalid response."
  []
  (fn [^Exception e ex-data _]
    (let [spec (:spec ex-data)
          body (:response ex-data)]
      (trace e "Invalid response")
      (error "Invalid response" (s/explain spec body))
      (status/internal-server-error
       {:message const/server-error-message
        :details (s/explain-str spec body)}))))

(defn api-routes
  "Define top-level API routes."
  [datasource config]
  (api
   (context "/api" []
     :coercion :spec
     (gymnastics-routes datasource)
     (presentations-routes datasource)
     (materials-routes datasource)
     (upload-routes config))))

;; Reitit
(def ^:private allowed-headers
  ["Origin"
   "Accept"
   "Content-Type"
   "Authorization"
   "X-Requested-With"
   "Cache-Control"])

(defn router-options
  [db config]
  (let [auth-backend (config/auth-backend config)]
    {:data {:app-config config
            :db         db
            :muuntaja   m/instance
            :coercion   rcs/coercion
            :middleware [parameters/parameters-middleware
                         muuntaja/format-middleware
                         error/exception-middleware
                         rrc/coerce-request-middleware
                         rrc/coerce-response-middleware
                         multipart/multipart-middleware
                         [wrap-cors
                          :access-control-allow-origin [#".*"]
                          :access-control-allow-headers allowed-headers
                          :access-control-allow-methods [:get :post :patch :put :delete]]
                         config/app-config-middleware
                         db-mw/db-middleware
                         [wrap-authentication auth-backend]]}}))

(defn routes
  []
  [["" {:no-doc    true
        :responses {200 {:schema any?}}
        :swagger
        {:basePath "/"
         :info
         {:title   "Education API"
          :version "1.6.0"}
         :securityDefinitions
         {:api_key
          {:type "apiKey"
           :name "Authorization"
           :in   "header"}}
         :tags     [{:name "users" :description "Users management"}
                    {:name "roles" :description "Roles management"}
                    {:name "lessons" :description "Video lessons"}
                    {:name "dresses" :description "Dresses rent"}
                    {:name "gymnastics" :description "Different gymnastics for children"}
                    {:name "presentations" :description "Interactive presentations"}
                    {:name "upload" :description "Upload media files"}
                    {:name "materials" :description "Download materials for education"}]}}
    ["/swagger.json" {:get (swagger/create-swagger-handler)}]
    ["/swagger/*" {:get (swagger-ui/create-swagger-ui-handler)}]]
   ["/api"
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
                :handler    users/login-handler}}]
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
              :middleware [[auth/require-roles #{:admin}]]
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
                :handler     users/delete-user-handler}}]]
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
                :handler    roles/roles-handler}}]
    ["/free-lesson"
     {:swagger {:tags ["lessons"]}
      :get     {:summary     "Get free lesson video"
                :description "Return video file with free lesson video"
                :parameters  {:query {:token string?}}
                :responses   {200 {:description "Successful"}
                              403 {:description const/no-access-error-message
                                   :body        ::specs-common/error-response}}
                :handler     lessons/serve-video-handler}
      :post    {:summary     "Request free video lesson"
                :description "Request free video lesson by `email`"
                :parameters  {:body ::specs-common/free-lesson-request}
                :responses   {204 {:description "Succesful"}
                              400 {:description const/bad-request-error-message
                                   :body        ::specs-common/error-response}}
                :handler     lessons/request-free-lesson-handler}}]
    ["/lessons"
     {:swagger {:tags ["lessons"]}}
     [""
      {:post {:swagger    {:security [{:api_key []}]}
              :summary    "Create new lesson"
              :middleware [[auth/require-roles #{:admin}]]
              :parameters {:body ::specs-common/lesson-create-request}
              :responses  {201 {:description "Lesson created successfully"
                                :body        ::specs-common/create-response}
                           400 {:description const/bad-request-error-message
                                :body        ::specs-common/error-response}
                           401 {:description const/not-authorized-error-message
                                :body        ::specs-common/error-response}
                           403 {:description const/no-access-error-message
                                :body        ::specs-common/error-response}}
              :handler    lessons/create-lesson-handler}
       :get  {:summary     "Get all lessons"
              :description "Get list of lessons with give `limit` and `offset`"
              :parameters  {:query ::specs-common/optional-limit-offset}
              :responses   {200 {:description "Successful"
                                 :body        ::specs-common/lessons-response}
                            400 {:description const/bad-request-error-message
                                 :body        ::specs-common/error-response}}
              :handler     lessons/get-all-lessons-handler}}]
     ["/:lesson-id"
      {:get    {:summary     "Get lesson"
                :description "Get lesson by `:lesson-id`"
                :parameters  {:path {:lesson-id ::specs-common/id}}
                :responses   {200 {:description "User was found in the database"
                                   :body        ::specs-common/lesson-response}
                              400 {:description const/bad-request-error-message
                                   :body        ::specs-common/error-response}
                              404 {:description const/not-found-error-message
                                   :body        ::specs-common/error-response}}
                :handler     lessons/get-lesson-by-id-handler}
       :patch  {:swagger     {:security [{:api_key []}]}
                :summary     "Update lesson"
                :description "Update existing lesson by `:lesson-id`"
                :middleware  [[auth/require-roles #{:admin}]]
                :parameters  {:path {:lesson-id ::specs-common/id}
                              :body ::specs-common/lesson-update-request}
                :responses   {204 {:description "Successful"}
                              400 {:description const/bad-request-error-message
                                   :body        ::specs-common/error-response}
                              401 {:description const/not-authorized-error-message
                                   :body        ::specs-common/error-response}
                              403 {:description const/no-access-error-message
                                   :body        ::specs-common/error-response}
                              404 {:description const/not-found-error-message
                                   :body        ::specs-common/error-response}}
                :handler     lessons/update-lesson-handler}
       :delete {:swagger     {:security [{:api_key []}]}
                :summary     "Delete lesson"
                :description "Delete existing lesson by `:lesson-id`"
                :middleware  [[auth/require-roles #{:admin}]]
                :parameters  {:path {:lesson-id ::specs-common/id}}
                :responses   {204 {:description "Successful"}
                              400 {:description const/bad-request-error-message
                                   :body        ::specs-common/error-response}
                              401 {:description const/not-authorized-error-message
                                   :body        ::specs-common/error-response}
                              403 {:description const/no-access-error-message
                                   :body        ::specs-common/error-response}
                              404 {:description const/not-found-error-message
                                   :body        ::specs-common/error-response}}
                :handler     lessons/delete-lesson-by-id-handler}}]]
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
                :responses   {400 {:description const/bad-request-error-message
                                   :body        ::specs-common/error-response}
                              401 {:description const/not-authorized-error-message
                                   :body        ::specs-common/error-response}
                              403 {:description const/no-access-error-message
                                   :body        ::specs-common/error-response}
                              404 {:description const/not-found-error-message
                                   :body        ::specs-common/error-response}}
                :handler     dresses/delete-dress-by-id-handler}}]]]])

(def prod-routes
  (constantly (routes)))
