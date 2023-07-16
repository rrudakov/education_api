(ns education.config
  (:require
   [aero.core :refer [read-config]]
   [buddy.auth.backends.token :refer [jws-backend]]
   [buddy.core.codecs :as codecs]
   [buddy.core.hash :as hash]
   [clojure.java.io :as io]))

(defn config
  "Read application config from resources."
  [profile]
  (read-config (io/resource "config/config.edn") {:profile profile}))

(defn application-port
  "Return application port value from `config`."
  [config]
  (get-in config [:app :port]))

(defn token-sign-secret
  "Return token sign secret."
  [config]
  (get-in config [:app :tokensign]))

(defn auth-backend
  "Return authentication back-end."
  [config]
  (jws-backend {:secret     (token-sign-secret config)
                :token-name "Token"
                :options    {:alg :hs512}}))

(defn db-config
  "Return database specification from the `config`."
  [config]
  (:database config))

(defn db-spec
  "Return database specification for pooled connection."
  [config]
  (let [db-uri (:url (db-config config))]
    {:jdbc-url      db-uri
     :pool-name     "education-pool"
     :max-pool-size 30}))

(defn base-url
  "Return BASE_URL for current application."
  [config]
  (get-in config [:app :base_url]))

(defn storage-path
  "Return path to the file storage."
  [config]
  (get-in config [:app :storage]))

(defn crypto-key
  [config]
  (hash/sha256 (get-in config [:app :crypto :key])))

(defn crypto-iv
  [config]
  (codecs/str->bytes (get-in config [:app :crypto :iv])))

(defn send-grid-base-url
  [config]
  (get-in config [:app :send-grid :base-url]))

(defn send-grid-api-key
  [config]
  (get-in config [:app :send-grid :api-key]))

(defn free-lesson-template-id
  [config]
  (get-in config [:app :send-grid :templates :free-lesson]))

(defn video-lessons-root-path
  [config]
  (get-in config [:app :video-lessons :root-path]))

(defn free-lesson-path
  [config]
  (get-in config [:app :video-lessons :free-lesson-path]))

(defn ulog-publishers
  [config]
  (get-in config [:app :logging :ulog]))

(def app-config-middleware
  {:name    ::app-config
   :compile (fn [{:keys [app-config]} _]
              (fn [handler]
                (fn [request]
                  (handler (assoc request :app-config app-config)))))})
