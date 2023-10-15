(ns education.specs.common
  (:require [clojure.spec.alpha :as s]
            [education.http.constants :as const]
            [education.specs.gymnastics :as g]
            [clojure.string :as str]))

(s/def ::id pos-int?)

(s/def ::email (s/and string? #(re-matches const/valid-email-regex %)))

(s/def ::subtype_id pos-int?)

(s/def ::title
  (s/and string?
         (complement str/blank?)
         #(<= (count %) 500)))

(s/def ::subtitle
  (s/and string?
         (complement str/blank?)
         #(<= (count %) 500)))

(s/def ::url
  (s/and string?
         (complement str/blank?)
         (partial re-matches const/valid-url-regex)
         #(<= (count %) 1000)))

;; Aliases for ::url
(s/def ::picture ::url)
(s/def ::screenshot ::url)
(s/def ::attachment ::url)
(s/def ::preview ::url)
(s/def ::store_link ::url)

(s/def ::pictures
  (s/coll-of ::picture :kind vector? :distinct true :into []))

(s/def ::screenshots
  (s/coll-of ::screenshot :kind vector? :distinct true :into []))

(s/def ::description
  (s/and string?
         (complement str/blank?)))

(s/def ::price
  (s/and string?
         (complement str/blank?)
         (partial re-matches const/valid-decimal)))

(s/def ::size pos-int?)

(s/def ::is_public boolean?)

(s/def ::created_on inst?)

(s/def ::updated_on inst?)

(s/def ::offset pos-int?)

(s/def ::limit pos-int?)

(s/def ::optional-limit-offset
  (s/keys :opt-un [::limit ::offset]))

(s/def ::create-response
  (s/keys :req-un [::id]))

;;; Lessons

(s/def ::lesson-create-request
  (s/keys :req-un [::title ::subtitle ::description ::screenshots ::price]))

(s/def ::lesson-update-request
  (s/keys :opt-un [::title ::subtitle ::description ::screenshots ::price]))

(s/def ::lesson-response
  (s/keys :req-un [::id
                   ::title
                   ::subtitle
                   ::description
                   ::screenshots
                   ::price
                   ::created_on
                   ::updated_on]))

(s/def ::lessons-response
  (s/coll-of ::lesson-response :kind vector? :distinct true :into []))

(s/def ::free-lesson-request
  (s/keys :req-un [::email]))

;;; Gymnastics

(s/def ::gymnastic-create-request
  (s/keys :req-un [::subtype_id ::title ::description]
          :opt-un [::g/picture]))

(s/def ::gymnastic-update-request
  (s/keys :req-un [::subtype_id ::title ::description]
          :opt-un [::g/picture]))

(s/def ::gymnastic-response
  (s/keys :req-un [::id ::subtype_id ::title ::description ::created_on ::updated_on]
          :opt-un [::g/picture]))

(s/def ::gymnastics-all-query-params
  (s/keys :req-un [::subtype_id]
          :opt-un [::limit ::offset]))

(s/def ::gymnastics-response
  (s/coll-of ::gymnastic-response :kind vector? :distinct true :into []))

;;; Dresses

(s/def ::dress-create-request
  (s/keys :req-un [::title ::description ::size ::pictures ::price]))

(s/def ::dress-update-request
  (s/keys :opt-un [::title ::description ::size ::pictures ::price]))

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

;;; Presentations

(s/def ::presentation-create-request
  (s/keys :req-un [::title ::url ::description]
          :opt-un [::attachment ::is_public ::preview ::subtype_id]))

(s/def ::presentation-update-request
  (s/keys :opt-un [::title ::url ::description ::attachment ::preview ::is_public ::subtype_id]))

(s/def ::presentations-all-query-parameters
  (s/keys :req-un [::subtype_id]
          :opt-un [::limit ::offset]))

(s/def ::presentation-response
  (s/keys :req-un [::id ::title ::description ::is_public ::created_on ::updated_on ::subtype_id]
          :opt-un [::url ::attachment ::preview]))

(s/def ::presentations-response
  (s/coll-of ::presentation-response :kind vector? :distinct true :into []))

;;; Materials

(s/def ::material-create-request
  (s/keys :req-un [::title ::description ::preview ::store_link ::price]))

(s/def ::material-update-request
  (s/keys :opt-un [::title ::description ::preview ::store_link ::price]))

(s/def ::material-response
  (s/keys :req-un [::id
                   ::title
                   ::description
                   ::preview
                   ::store_link
                   ::price
                   ::created_on
                   ::updated_on]))

(s/def ::materials-response
  (s/coll-of ::material-response :kind vector? :distinct true :into []))

;;; Errors

(s/def ::message string?)

(s/def ::error_code string?)

(s/def ::details string?)

(s/def ::error-response
  (s/keys :req-un [::message]
          :opt-un [::error_code ::details]))
