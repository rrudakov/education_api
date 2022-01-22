(ns education.specs.upload
  (:require
   [clojure.spec.alpha :as s]
   [education.http.constants :as const]))

(s/def ::url (s/and string? not-empty (partial re-matches const/valid-url-regex)))

(s/def ::upload-response (s/keys :req-un [::url]))
