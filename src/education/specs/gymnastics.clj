(ns education.specs.gymnastics
  (:require [clojure.spec.alpha :as s]
            [education.http.constants :as const]))

(s/def ::picture
  (s/nilable
   (s/and string?
          not-empty
          (partial re-matches const/valid-url-regex)
          #(<= (count %) 1000))))
