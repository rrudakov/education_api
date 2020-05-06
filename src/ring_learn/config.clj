(ns ring-learn.config
  (:require [aero.core :refer [read-config]]
            [buddy.auth :refer [throw-unauthorized]]
            [buddy.auth.backends.token :refer [jws-backend]]
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
  (jws-backend {:secret (token-sign-secret config)
                :token-name "Token"
                :options {:alg :hs512}}))

(defn db-spec
  "Return database specification from the `config`."
  [config]
  (:database config))