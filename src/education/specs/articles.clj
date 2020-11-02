(ns education.specs.articles
  (:require [clojure.spec.alpha :as s]))

(s/def ::id pos-int?)

(s/def ::user_id pos-int?)

(s/def ::user-id-param pos-int?)

(s/def ::title (s/and string? not-empty #(<= (count %) 100)))

(s/def ::body (s/and string? not-empty))

(s/def ::featured_image (s/and string? #(<= (count %) 500)))

(s/def ::is_main_featured boolean?)

(s/def ::created_on inst?)

(s/def ::updated_on inst?)

(s/def ::description string?)

(s/def ::article-create-request
  (s/keys :req-un [::title ::body ::featured_image ::is_main_featured] :opt-un [::description]))

(s/def ::article-update-request
  (s/keys :opt-un [::title ::body ::featured_image ::is_main_featured ::description]))

(s/def ::article-create-response
  (s/keys :req-un [::id]))

(s/def ::article-short
  (s/keys :req-un [::id ::user_id ::title ::featured_image ::updated_on] :opt-un [::description]))

(s/def ::articles-short
  (s/coll-of ::article-short :kind vector? :distinct true :into []))

(s/def ::article-full
  (s/keys :req-un [::id ::user_id ::title ::body ::featured_image ::created_on ::updated_on ::description]))

(s/def ::articles-full
  (s/coll-of ::article-full :kind vector? :distinct true :into []))

(s/def ::limit pos-int?)
