(ns education.specs.users
  (:require [clojure.spec.alpha :as s]
            [education.http.constants :as const]))

(s/def ::id int?)

(s/def ::username
  (s/and string?
         #(re-matches const/valid-username-regex %)
         #(>= (count %) 2)))

(s/def ::password (s/and string? #(>= (count %) 6)))

(s/def ::email (s/and string? #(re-matches const/valid-email-regex %)))

(s/def ::role #{"admin" "moderator" "guest"})

(s/def ::roles (s/coll-of ::role :kind vector? :into [] :distinct true :min-count 1))

(s/def ::created_on inst?)

(s/def ::updated_on inst?)

(s/def ::token string?)

(s/def ::user-create-request
  (s/keys :req-un [::username ::password ::email]))

(s/def ::user-update-request
  (s/keys :req-un [::roles]))

(s/def ::user-create-response
  (s/keys :req-un [::id]))

(s/def ::user-response
  (s/keys :req-un [::id ::username ::email ::roles ::created_on ::updated_on]))

(s/def ::users-response
  (s/coll-of ::user-response :kind vector? :distinct true :into []))

(s/def ::login-request
  (s/keys :req-un [::username ::password]))

(s/def ::token-response
  (s/keys :req-un [::token]))
