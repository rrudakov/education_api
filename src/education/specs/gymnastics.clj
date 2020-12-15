(ns education.specs.gymnastics
  (:require [clojure.spec.alpha :as s]
            [education.http.constants :as const]))

(s/def ::id pos-int?)

(s/def ::title (s/and string? not-empty #(<= (count %) 500)))

(s/def ::description (s/and string? not-empty))

(s/def ::picture (s/and string? not-empty (partial re-matches const/valid-url-regex) #(<= (count %) 1000)))

(s/def ::created_on inst?)

(s/def ::updated_on inst?)

(s/def ::limit pos-int?)

(s/def ::offset pos-int?)

(s/def ::gymnastic-create-request
  (s/keys :req-un [::title ::description ::picture]))

(s/def ::gymnastic-update-request
  (s/keys :opt-un [::title ::description ::picture]))

(s/def ::gymnastic-create-response
  (s/keys :req-un [::id]))

(s/def ::gymnastic-response
  (s/keys :req-un [::id ::title ::description ::picture ::created_on ::updated_on]))

(s/def ::gymnastics-response
  (s/coll-of ::gymnastic-response :kind vector? :distinct true :into []))
