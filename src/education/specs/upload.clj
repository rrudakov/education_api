(ns education.specs.upload
  (:require [clojure.spec.alpha :as s]
            [spec-tools.core :as st]
            [education.http.constants :as const]))

(s/def ::upload
  (st/spec {:json-schema/type "file"
            :swagger/in       "formData"
            :swagger/name     "file"
            :description      "The media file to upload"}))

(s/def ::url (s/and string? not-empty (partial re-matches const/valid-url-regex)))

(s/def ::upload-response (s/keys :req-un [::url]))
