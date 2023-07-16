(ns education.http.endpoints.test-app
  (:require
   [cheshire.generate :refer [add-encoder]]
   [cljc.java-time.format.date-time-formatter :as dtf]
   [clojure.data.json :as json]
   [education.http.routes :as r]
   [education.test-data :refer [test-config]]
   [reitit.ring :as ring]
   [clojure.java.io :as io]))

(defn api-routes-with-auth
  "Return configured application."
  []
  (add-encoder
   java.time.Instant
   (fn [c json-generator]
     (.writeString json-generator (dtf/format dtf/iso-instant c))))
  (ring/ring-handler
   (ring/router (r/routes) (r/router-options nil test-config :hato-client))
   (ring/routes
    (ring/redirect-trailing-slash-handler)
    (ring/create-default-handler
     {:not-found          (constantly {:status 404 :body "Not found"})
      :method-not-allowed (constantly {:status 405 :body "Method not allowed"})
      :not-acceptable     (constantly {:status 406 :body "Not acceptable"})}))))

(defn parse-body
  "Parse response body into clojure map."
  [body]
  (with-open [r (io/reader body)]
    (json/read r :key-fn keyword)))
