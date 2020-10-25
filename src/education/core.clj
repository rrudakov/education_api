(ns education.core
  (:gen-class)
  (:require [com.stuartsierra.component :as component]
            [education.system :refer [api-system]]
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
  (component/start (api-system :prod)))
