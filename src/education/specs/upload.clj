(ns education.specs.upload
  (:require [clojure.spec.alpha :as s]
            [spec-tools.core :as st]))

(s/def ::filename string?)

(s/def ::content-type string?)

(s/def ::size pos-int?)

(s/def ::tempfile (partial instance? java.io.File))

(s/def ::file
  (st/spec
   {:spec
    (s/keys :req-un [::filename ::content-type ::size] :opt-un [::tempfile])
    :json-schema/type "file"}))
