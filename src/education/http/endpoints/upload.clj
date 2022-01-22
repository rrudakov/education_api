(ns education.http.endpoints.upload
  (:require
   [clojure.java.io :refer [file input-stream output-stream]]
   [clojure.string :as str]
   [education.config :as config]
   [education.utils.path :as path]
   [ring.util.http-response :as status]))

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

(defn- extract-file-extension
  [filename]
  (if (str/includes? filename ".")
    (str "." (last (str/split filename #"\.")))
    ""))

(defn upload-file-handler
  [{:keys [app-config] {{{:keys [filename tempfile]} :file} :multipart} :parameters}]
  (let [extension (extract-file-extension filename)
        name      (str/join [(uuid) extension])
        path      (path/join (config/storage-path app-config) img-prefix name)]
    (write-file tempfile path)
    (status/ok {:url (path/join (config/base-url app-config) img-prefix name)})))
