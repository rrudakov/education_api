(ns education.http.endpoints.upload
  (:require [clojure.java.io :refer [file input-stream output-stream]]
            [clojure.string :as str]
            [compojure.api.sweet :refer [POST]]
            [education.config :as config]
            [education.specs.upload :as specs]
            [education.utils.path :as path]
            [ring.middleware.multipart-params :as mw]
            [ring.util.http-response :refer [ok]]))

(def ^:private img-prefix "img")

(defn uuid []
  (str (java.util.UUID/randomUUID)))

(defn file->byte-array
  [f]
  (let [buf (byte-array (.length f))]
    (with-open [in (input-stream f)]
      (.read in buf))
    buf))

(defn write-file
  [f name]
  (with-open [out (output-stream (file name))]
    (.write out (file->byte-array f))))

(defn upload-file-handler
  [{:keys [filename content-type tempfile]} config]
  (let [name (str/join "_" [(uuid) filename])
        path (path/join (config/storage-path config) img-prefix name)]
    (write-file tempfile path)
    (ok {:url (path/join (config/base-url config) img-prefix name)})))

(defn upload-routes
  "Define routes for file upload."
  [config]
  (POST "/upload" []
    :tags ["upload"]
    :multipart-params [file :- ::specs/file]
    :middleware [mw/wrap-multipart-params]
    :summary "Upload any file and get link to it"
    :responses {200 {:description "Successful"
                     :schema ::specs/upload-response}}
    (upload-file-handler file config)))
