(ns education.specs.gymnastics
  (:require [clojure.spec.alpha :as s]
            [education.http.constants :as const]
            [clojure.string :as str]))

(s/def ::picture
  (s/and string?
         (complement str/blank?)
         (partial re-matches const/valid-url-regex)
         #(<= (count %) 1000)))
