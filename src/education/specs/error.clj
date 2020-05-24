(ns education.specs.error
  (:require [clojure.spec.alpha :as s]))

(s/def ::message string?)

(s/def ::error_code string?)

(s/def ::details string?)

(s/def ::error-response (s/keys :req-un [::message] :opt-un [::error_code ::details]))
