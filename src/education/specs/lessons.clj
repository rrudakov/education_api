(ns education.specs.lessons
  (:require [clojure.spec.alpha :as s]
            [education.http.constants :as const]))

(s/def ::id pos-int?)

(s/def ::title (s/and string? not-empty #(<= (count %) 500)))

(s/def ::subtitle (s/and string? not-empty #(<= (count %) 500)))

(s/def ::description (s/and string? not-empty))

(s/def ::screenshot (s/and string?
                           not-empty
                           (partial re-matches const/valid-url-regex)
                           #(<= (count %) 1000)))

(s/def ::screenshots (s/coll-of ::screenshot :kind vector? :distinct true :into []))

(s/def ::price (s/and string? not-empty (partial re-matches const/valid-decimal)))

(s/def ::created_on inst?)

(s/def ::updated_on inst?)

(s/def ::lesson-create-request
  (s/keys :req-un [::title ::subtitle ::description ::screenshots ::price]))

(s/def ::lesson-update-request
  (s/keys :opt-un [::title ::subtitle ::description ::screenshots ::price]))

(s/def ::lesson-created-response
  (s/keys :req-un [::id]))

(s/def ::lesson-response
  (s/keys :req-un [::id ::title ::subtitle ::description ::screenshots ::price ::created_on ::updated_on]))

(s/def ::lessons-response (s/coll-of ::lesson-response :kind vector? :distinct true :into []))

(s/def ::limit (s/or :default nil? :specified pos-int?))

(s/def ::offset (s/or :default nil? :specified pos-int?))
