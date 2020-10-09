(ns education.specs.lessons
  (:require [clojure.spec.alpha :as s]))

(def ^:private valid-url
  "Regex to check video and image URLs."
  #"((http|https)://)(www.)*[a-zA-Z0-9@:%._\+~#?&//=]{2,256}\.[a-z]{2,6}\b([-a-zA-Z0-9@:%._\+~#?&//=]*)")

(def ^:private valid-decimal
  "Regex to check price."
  #"\d+(\.\d+)?")

(s/def ::id int?)

(s/def ::title (s/and string? not-empty #(<= (count %) 500)))

(s/def ::subtitle (s/and string? not-empty #(<= (count %) 500)))

(s/def ::description (s/and string? not-empty))

(s/def ::screenshot (s/and string? not-empty (partial re-matches valid-url)))

(s/def ::screenshots (s/coll-of ::screenshot :kind vector? :distinct true :into []))

(s/def ::price (s/and string? not-empty (partial re-matches valid-decimal)))

(s/def ::created_on inst?)

(s/def ::updated_on inst?)

(s/def ::lesson-create-request
  (s/keys :req-un [::title ::subtitle ::description ::screenshots ::price]))

(s/def ::lesson-update-request
  (s/keys :opt-un [::title ::subtitle ::description ::screenshots ::price]))

(s/def ::lesson-response
  (s/keys :req-un [::id ::title ::subtitle ::description ::screenshots ::price ::created_on ::updated_on]))

(s/def ::lessons-response (s/coll-of ::lesson-response :kind vector? :distinct true :into []))

(s/def ::limit (s/or :default nil? :specified int?))

(s/def ::offset (s/or :default nil? :specified int?))
