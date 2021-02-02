(ns education.specs.gymnastics-test
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [education.specs.gymnastics :as sut]))

(deftest picture-test
  (doseq [picture ["http://insecure.com"
                   "https://secure.net/some.img.jpg"
                   "https://some.url/"
                   "http://127.0.0.1:3000/some_image.jpg"
                   (str "https://some.url/" (str/join (repeat 979 "a")) ".jpg")
                   nil]]
    (testing (str "::picture is valid " picture)
      (is (s/valid? ::sut/picture picture))))

  (doseq [picture ["juststring"
                   "www.picture.com/image.jpg"
                   (str "https://some.url/" (str/join (repeat 980 "a")) ".jpg")]]
    (testing (str "::picture is invalid " picture)
      (is (not (s/valid? ::sut/picture picture))))))
