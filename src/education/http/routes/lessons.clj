(ns education.http.routes.lessons
  (:require [education.http.constants :as const]
            [education.specs.common :as specs-common]
            [education.http.endpoints.lessons :as lessons]
            [education.http.middleware :as auth]))

(defn lessons-routes
  []
  [["/free-lesson"
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
               :handler     lessons/delete-lesson-by-id-handler}}]]])
