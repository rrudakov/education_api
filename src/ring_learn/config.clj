(ns ring-learn.config
  (:require [aero.core :refer [read-config]]
            [clojure.java.io :as io]))

(defn config
  "Read application config from resources."
  [profile]
  (read-config (io/resource "config/config.edn") {:profile profile}))

(defn application-port
  "Return application port value from `config`."
  [config]
  (get-in config [:app :port]))

(defn db-spec
  "Return database specification from the `config`."
  [config]
  (:database config))
