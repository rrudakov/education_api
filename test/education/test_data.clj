(ns education.test-data
  (:require  [clojure.test :as t]
             [buddy.sign.jwt :as jwt]
             [education.config :as config]
             [cheshire.core :as cheshire])
  (:import java.time.Instant))

(def test-config
  "Mocked test configuration for using in unit tests."
  {:app
   {:tokensign "test_secret"}})

(def auth-user
  "Valid authorized user."
  {:id 42
   :username "admin"
   :email "admin@example.com"
   :created_on (Instant/now)
   :updated_on (Instant/now)})

(defn test-auth-token
  "Generate valid authorization token."
  [roles]
  (let [exp   (.plusSeconds (Instant/now) (* 60 60 24))
        token (-> {:user (assoc auth-user :roles roles)}
                  (assoc :exp exp)
                  (jwt/sign (config/token-sign-secret test-config) {:alg :hs512}))]
    (str "Token " token)))

(def auth-user-deserialized
  "Authorized user deserialized from authorization token."
  (cheshire/parse-string (cheshire/generate-string auth-user) true))
