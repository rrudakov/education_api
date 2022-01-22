(ns education.http.routes.upload
  (:require
   [education.http.constants :as const]
   [education.http.endpoints.upload :as upload]
   [education.specs.common :as specs-common]
   [education.specs.upload :as specs-upload]
   [reitit.ring.middleware.multipart :as multipart]))

(defn upload-routes
  []
  ["/upload"
   {:post {:swagger    {:tags ["upload"]}
           :summary    "Upload any file and get link to it"
           :parameters {:multipart {:file multipart/temp-file-part}}
           :responses  {200 {:description "File upload successful"
                             :body        ::specs-upload/upload-response}
                        400 {:description const/bad-request-error-message
                             :body        ::specs-common/error-response}}
           :handler    upload/upload-file-handler}}])
