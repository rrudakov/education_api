(ns education.specs.upload
  (:require [clojure.spec.alpha :as s]
            [spec-tools.core :as st]
            [education.http.constants :as const]))

(s/def ::filename string?)

(s/def ::content-type string?)

(s/def ::size pos-int?)

(s/def ::tempfile (partial instance? java.io.File))

(s/def ::file
  (st/spec
   {:spec
    (s/keys :req-un [::filename ::content-type ::size] :opt-un [::tempfile])
    :json-schema/type "file"}))

(s/def ::url (s/and string? not-empty (partial re-matches const/valid-url-regex)))

(s/def ::upload-response
  (s/keys :req-un [::url]))
