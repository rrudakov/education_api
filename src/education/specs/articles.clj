(ns education.specs.articles
  (:require [clojure.spec.alpha :as s]))

(s/def ::id int?)

(s/def ::user_id int?)

(s/def ::user-id-param (s/or :user int?
                             :all nil))

(s/def ::title
  (s/and string? #(<= (count %) 100) #(>= (count %) 1)))

(s/def ::body
  (s/and string? #(>= (count %) 1)))

(s/def ::featured_image
  (s/and string? #(<= (count %) 500)))

(s/def ::is_main_featured boolean?)

(s/def ::created_on inst?)

(s/def ::updated_on inst?)

(s/def ::description string?)

(s/def ::article-create-request
  (s/keys :req-un [::title ::body ::featured_image ::is_main_featured] :opt-un [::description]))

(s/def ::article-update-request
  (s/keys :opt-un [::title ::body ::featured_image ::is_main_featured ::description]))

(s/def ::article-short
  (s/keys :req-un [::id ::user_id ::title ::featured_image ::updated_on] :opt-un [::description]))

(s/def ::articles-short
  (s/coll-of ::article-short :kind set? :into #{}))

(s/def ::article-full
  (s/keys :req-un [::id ::user_id ::title ::body ::featured_image ::created_on ::updated_on ::description]))

(s/def ::articles-full
  (s/coll-of ::article-full :kind set? :into #{}))

(s/def ::limit int?)
