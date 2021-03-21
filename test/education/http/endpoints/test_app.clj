(ns education.http.endpoints.test-app
  (:require [buddy.auth.middleware :refer [wrap-authentication wrap-authorization]]
            [compojure.api.api :refer [api-defaults]]
            [education.config :as config]
            [education.http.routes :as routes]
            [education.test-data :refer [test-config]]
            [jsonista.core :as j]
            [muuntaja.middleware :refer [wrap-format]]
            [ring.middleware.cors :refer [wrap-cors]]
            [ring.middleware.defaults :refer [wrap-defaults]]))

(defn api-routes-with-auth
  "Return configured application with authorization middleware."
  []
  (let [auth-backend (config/auth-backend test-config)]
    (-> (routes/api-routes nil test-config)
        (wrap-cors :access-control-allow-origin [#".*"]
                   :access-control-allow-headers ["Origin" "Accept" "Content-Type" "Authorization" "X-Requested-With" "Cache-Control"]
                   :access-control-allow-methods [:get :post :patch :put :delete])
        (wrap-authorization auth-backend)
        (wrap-authentication auth-backend)
        (wrap-format)
        (wrap-defaults api-defaults))))

(defn parse-body
  "Parse response body into clojure map."
  [body]
  (-> body
      slurp
      (j/read-value j/keyword-keys-object-mapper)))
