(ns education.core
  (:gen-class)
  (:require
   [education.system :refer [system-config-prod]]
   [integrant.core :as ig]))

(defn -main
  "Run HTTP server."
  [& _args]
  (ig/init system-config-prod))
