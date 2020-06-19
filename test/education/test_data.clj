(ns education.test-data
  (:require [buddy.sign.jwt :as jwt]
            [cheshire.core :as cheshire]
            [education.config :as config]
            [buddy.hashers :as hs])
  (:import java.time.Instant))

(def test-config
  "Mocked test configuration for using in unit tests."
  {:app
   {:tokensign "test_secret"
    :port 3000}})

(def password
  "Password for test users."
  "123456")

(def add-user1-request
  "Create test user request."
  {:username "rrudakov"
   :email    "rrudakov@pm.me"
   :password password})

(def add-user2-request
  "Create test user request."
  {:username "rrudakov2"
   :email    "rrudakov@protonmail.com"
   :password password})

(def auth-user1-request
  "Authorize test user request."
  {:username (:username add-user1-request)
   :password password})

(def update-user1-request
  "Update test user request."
  {:roles ["admin" "moderator"]})

(def db-test-user1
  "First mocked testing user."
  {:users/id            4
   :users/user_name     (:username add-user1-request)
   :users/user_password (hs/encrypt password)
   :users/user_email    (:email add-user1-request)
   :users/created_on    (Instant/now)
   :users/updated_on    (Instant/now)})

(def db-test-user2
  "Second mocked testing user."
  {:users/id            11
   :users/user_name     (:username add-user2-request)
   :users/user_password (hs/encrypt password)
   :users/user_email    (:email add-user2-request)
   :users/created_on    (Instant/now)
   :users/updated_on    (Instant/now)})

(def db-all-roles
  "All roles from database."
  [{:roles/id 1 :roles/role_name :admin}
   {:roles/id 2 :roles/role_name :moderator}
   {:roles/id 3 :roles/role_name :guest}])

(def db-guest-role
  "Guest role for new registered user."
  {:roles/id        1
   :roles/role_name "guest"})

(def user1-roles
  "Roles for first mocked user."
  #{:admin :guest})

(def user2-roles
  "Roles for second mocked user."
  #{:admin :guest :moderator})

(def db-user-auth-successful
  "Returned after successful authentication."
  [true
   {:user (-> db-test-user1
              (dissoc :users/user_password)
              (assoc :users/roles user1-roles))}])

(def auth-user
  "Valid authorized user."
  {:id         (:users/id db-test-user1)
   :username   (:users/user_name db-test-user1)
   :email      (:users/user_email db-test-user1)
   :created_on (:users/created_on db-test-user1)
   :updated_on (:users/updated_on db-test-user1)})

(defn test-auth-token
  "Generate valid authorization token."
  [roles]
  (let [exp   (.plusSeconds (Instant/now) (* 60 60 24))
        token (-> {:user (assoc auth-user :roles roles)}
                  (assoc :exp exp)
                  (jwt/sign (config/token-sign-secret test-config) {:alg :hs512}))]
    (str "Token " token)))

(defn parse-token
  "Parse token with test configuration."
  [token]
  (jwt/unsign token (config/token-sign-secret test-config) {:alg :hs512}))

(def auth-user-deserialized
  "Authorized user deserialized from authorization token."
  (cheshire/parse-string (cheshire/generate-string auth-user) true))

(defn auth-user-deserialized-with-role
  "Return authorized user with given `role`."
  [role]
  (assoc auth-user-deserialized :roles (vector (name role))))
