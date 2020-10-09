(ns education.core
  (:gen-class)
  (:require [com.stuartsierra.component :as component]
            [education.system :refer [api-system]]))

(defn -main
  "Run HTTP server."
  [& args]
  (component/start (api-system :prod)))
