(ns ring-learn.core
  (:require [com.stuartsierra.component :as component]
            [ring-learn.system :refer [api-system]]))

(defn -main
  "Run HTTP server."
  [& args]
  (component/start (api-system :prod)))
