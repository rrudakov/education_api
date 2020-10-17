(ns education.http.endpoints.upload
  (:require [clojure.java.io :refer [file input-stream output-stream]]
            [compojure.api.sweet :refer [POST]]
            [education.specs.upload :as specs]
            [ring.middleware.multipart-params :as mw]
            [ring.util.http-response :refer [ok]]
            [education.config :as config]
            [clojure.string :as str]))

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
  (let [name (str/join "_" (vector (uuid) filename))
        path (str (config/storage-path config) name)]
    (write-file tempfile path)
    (ok {:name (str/join "/" (vector (config/base-url config) name))})))

(defn upload-routes
  "Define routes for file upload."
  [config]
  (POST "/upload" []
    :multipart-params [file :- ::specs/file]
    :middleware [mw/wrap-multipart-params]
    :summary "Upload any file and get link to it"
    (upload-file-handler file config)))
