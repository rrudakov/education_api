(ns education.specs.articles-test
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [education.specs.articles :as sut])
  (:import java.time.Instant))

(deftest id-test
  (doseq [id [1 33 99999999999]]
    (testing (str "::id is valid " id)
      (is (s/valid? ::sut/id id))))

  (doseq [id ["string" false -1]]
    (testing (str "::id is invalid " id)
      (is (not (s/valid? ::sut/id id))))))

(deftest user-id-test
  (doseq [user-id [1 33 9999999999]]
    (testing (str "::user-id is valid " user-id)
      (is (s/valid? ::sut/user_id user-id))))

  (doseq [user-id ["string"  false -99]]
    (testing (str "::user-id is invalid " user-id)
      (is (not (s/valid? ::sut/user_id user-id))))))

(deftest user-id-param-test
  (doseq [user-id-param [1 33 9999999999]]
    (testing (str "::user-id-param is valid " user-id-param)
      (is (s/valid? ::sut/user-id-param user-id-param))))

  (doseq [user-id-param ["string"  false -99]]
    (testing (str "::user-id-param is invalid " user-id-param)
      (is (not (s/valid? ::sut/user-id-param user-id-param))))))

(deftest title-test
  (doseq [title ["Any" "valid string" "8. Starting from number"]]
    (testing (str "::title is valid " title)
      (is (s/valid? ::sut/title title))))

  (doseq [title [123 true "" ["list" "of" "strings"] (str/join (repeat 101 "s"))]]
    (testing (str "::title is invalid " title)
      (is (not (s/valid? ::sut/title title))))))

(deftest body-test
  (doseq [body ["any string" (str/join " " (repeat 600 "very long string"))]]
    (testing (str "::body is valid " body)
      (is (s/valid? ::sut/body body))))

  (doseq [body ["" false 1234 []]]
    (testing (str "::body is invalid " body)
      (is (not (s/valid? ::sut/body body))))))

(deftest featured-image-test
  (doseq [featured-image ["Any" "" "valid string" "8. Starting from number"]]
    (testing (str "::featured-image is valid " featured-image)
      (is (s/valid? ::sut/featured_image featured-image))))

  (doseq [featured-image [123 true ["list" "of" "strings"] (str/join (repeat 501 "s"))]]
    (testing (str "::featured-image is invalid " featured-image)
      (is (not (s/valid? ::sut/featured_image featured-image))))))

(deftest is-main-featured-test
  (doseq [is-main-featured [true false]]
    (testing (str "::is-main-featured is valid " is-main-featured)
      (is (s/valid? ::sut/is_main_featured is-main-featured))))

  (doseq [is-main-featured ["string" 123 nil [] #{} :keyword]]
    (testing (str "::is-main-featured is invalid")
      (is (not (s/valid? ::sut/is_main_featured is-main-featured))))))

(deftest created-on-test
  (testing "::created_on is valid"
    (is (s/valid? ::sut/created_on (Instant/now))))

  (doseq [created-on ["2020-06-23T18:05:22.905106Z" "any_value" 1234567888 false]]
    (testing (str "::created_on is invalid " created-on)
      (is (not (s/valid? ::sut/created_on created-on))))))

(deftest updated-on-test
  (testing "::updated_on is valid"
    (is (s/valid? ::sut/updated_on (Instant/now))))

  (doseq [updated-on ["2020-06-23T18:05:22.905106Z" "any_value" 1234567888 false]]
    (testing (str "::updated_on is ivalid " updated-on)
      (is (not (s/valid? ::sut/updated_on updated-on))))))

(deftest description-test
  (doseq [description ["any string" "" (str/join " " (repeat 600 "very long string"))]]
    (testing (str "::description is valid " description)
      (is (s/valid? ::sut/description description))))

  (doseq [description [false 1234 []]]
    (testing (str "::description is invalid " description)
      (is (not (s/valid? ::sut/description description))))))

(deftest limit-test
  (doseq [limit [1 999999999]]
    (testing (str "::limit is valid " limit)
      (is (s/valid? ::sut/limit limit))))

  (doseq [limit [-1 0 "string" [] #{} false true]]
    (testing (str "::limit is invalid " limit)
      (is (not (s/valid? ::sut/limit limit))))))
