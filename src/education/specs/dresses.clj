(ns education.specs.dresses
  (:require [clojure.spec.alpha :as s]
            [education.http.constants :as const]))

(s/def ::id pos-int?)

(s/def ::title (s/and string? not-empty #(<= (count %) 500)))

(s/def ::description (s/and string? not-empty))

(s/def ::size pos-int?)

(s/def ::picture (s/and string?
                        not-empty
                        (partial re-matches const/valid-url-regex)
                        #(<= (count %) 1000)))

(s/def ::pictures (s/coll-of ::picture :kind vector? :distinct true :into []))

(s/def ::price (s/and string? not-empty (partial re-matches const/valid-decimal)))

(s/def ::created_on inst?)

(s/def ::updated_on inst?)

(s/def ::limit pos-int?)

(s/def ::offset pos-int?)

(s/def ::dress-create-request
  (s/keys :req-un [::title ::description ::size ::pictures ::price]))

(s/def ::dress-update-request
  (s/keys :opt-un [::title ::description ::size ::pictures ::price]))

(s/def ::dress-create-response
  (s/keys :req-un [::id]))

(s/def ::dress-response
  (s/keys :req-un [::id
                   ::title
                   ::description
                   ::size
                   ::pictures
                   ::price
                   ::created_on
                   ::updated_on]))

(s/def ::dresses-response
  (s/coll-of ::dress-response :kind vector? :distinct true :into []))
