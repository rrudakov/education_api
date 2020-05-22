(ns education.specs.users
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as str]))

(def email-regex
  "Check new email addresses using this regex."
  #"[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?")

(s/def ::id int?)

(s/def ::username string?)

(s/def ::password (s/and string? #(>= (count %) 6)))

(s/def ::email (s/and string? #(re-matches email-regex %)))

(s/def ::role #{:admin :moderator :guest})

(s/def ::roles (s/coll-of ::role :kind set?))

(s/def ::created_on inst?)

(s/def ::updated_on inst?)

(s/def ::token string?)

(s/def ::user-create-request (s/keys :req-un [::username ::password ::email]))

(s/def ::user-update-request (s/keys :req-un [::roles]))

(s/def ::user-response (s/keys :req-un [::id ::username ::email ::roles ::created_on ::updated_on]))

(s/def ::login-request (s/keys :req-un [::username ::password]))

(s/def ::token-response (s/keys :req-un [::token]))
