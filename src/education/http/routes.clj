(ns education.http.routes
  (:require [compojure.api.exception :as ex]
            [compojure.api.sweet :refer [api context]]
            [education.http.constants :as const]
            [education.http.endpoints.articles :refer [articles-routes]]
            [education.http.endpoints.dresses :refer [dresses-routes]]
            [education.http.endpoints.gymnastics :refer [gymnastics-routes]]
            [education.http.endpoints.lessons :refer [lessons-routes]]
            [education.http.endpoints.roles :refer [roles-routes]]
            [education.http.endpoints.upload :refer [upload-routes]]
            [education.http.endpoints.users :refer [users-routes]]
            [ring.util.http-response :as status])
  (:import java.sql.SQLException))

(defn sql-exception-handler
  "Database exception mapper."
  []
  (fn [^SQLException e _ _]
    (case (.getSQLState e)
      "23505" (status/conflict {:message const/conflict-error-message})
      "23503" (status/not-found {:message const/not-found-error-message})
      "23502" (status/bad-request {:message const/bad-request-error-message})
      (status/internal-server-error {:message    (str e)
                                     :error_code (.getSQLState e)}))))

(defn request-validation-handler
  "Verify request body and raise error."
  []
  (fn [^Exception e _ _]
    (status/bad-request {:message const/bad-request-error-message
                         :details (.getMessage e)})))

(defn response-validation-handler
  "Return error in case of invalid response."
  []
  (fn [^Exception e _ _]
    (status/internal-server-error
     {:message const/server-error-message
      :details (.getMessage e)})))

(defn api-routes
  "Define top-level API routes."
  [datasource config]
  (api
   {:swagger
    {:ui   "/swagger"
     :spec "/swagger.json"
     :options
     {:ui   {}
      :spec {}}
     :data
     {:info
      {:version     "0.1-alpha"
       :title       "Education API"
       :description "REST API for education application"
       :contact
       {:name  "Roman Rudakov"
        :email "rrudakov@pm.me"}}
      :tags [{:name "users" :description "Users management"}
             {:name "roles" :description "Roles management"}
             {:name "articles" :description "Articles management"}
             {:name "lessons" :description "Video lessons"}
             {:name "dresses" :description "Dresses rent"}
             {:name "gymnastics" :description "Different gymnastics for children"}
             {:name "upload" :description "Upload media files"}]
      :securityDefinitions
      {:api_key
       {:type "apiKey"
        :name "Authorization"
        :in   "header"}}}}
    :exceptions
    {:handlers
     {SQLException             (sql-exception-handler)
      ::ex/request-validation  (request-validation-handler)
      ::ex/response-validation (response-validation-handler)}}}

   (context "/api" []
     :coercion :spec
     (users-routes datasource config)
     (articles-routes datasource)
     (roles-routes datasource)
     (lessons-routes datasource)
     (dresses-routes datasource)
     (gymnastics-routes datasource)
     (upload-routes config))))
