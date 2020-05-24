(ns education.specs.roles
  (:require [clojure.spec.alpha :as s]))

(s/def ::id int?)

(s/def ::name #{:admin :moderator :guest})

(s/def ::role (s/keys :req-un [::id ::name]))

(s/def ::roles-response (s/coll-of ::role :kind set?))
