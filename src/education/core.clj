(ns education.core
  (:gen-class)
  (:require [education.system :refer [system-config]]
            [integrant.core :as ig]
            [next.jdbc.result-set :as rs])
  (:import java.sql.Array))

;; Fetch arrays from database as vectors
(extend-protocol rs/ReadableColumn
  Array
  (read-column-by-label
    [^Array v _]
    (vec (.getArray v)))

  (read-column-by-index
    [^Array v _ _]
    (vec (.getArray v))))

(defn -main
  "Run HTTP server."
  [& args]
  (ig/init (system-config :prod)))
