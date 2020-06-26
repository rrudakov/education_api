(ns education.http.endpoints.test-app
  (:require [buddy.auth.middleware :refer [wrap-authentication wrap-authorization]]
            [education.config :as config]
            [education.http.routes :as routes]
            [education.test-data :refer [test-config]]
            [cheshire.core :as cheshire]))

(defn test-api-routes-with-auth
  "Return configured application with authorization middleware."
  [db]
  (let [auth-backend (config/auth-backend test-config)]
    (-> (routes/api-routes db test-config)
        (wrap-authorization auth-backend)
        (wrap-authentication auth-backend))))

(defn parse-body
  "Parse response body into clojure map."
  [body]
  (cheshire/parse-string (slurp body) true))
