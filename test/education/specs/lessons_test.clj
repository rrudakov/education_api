(ns education.specs.lessons-test
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [education.specs.lessons :as sut])
  (:import java.time.Instant))

(deftest id-test
  (doseq [id [1 33 9999999999992222]]
    (testing (str "::id is valid " id)
      (is (s/valid? ::sut/id id))))

  (doseq [id ["string" false -1]]
    (testing (str "::id is invalid " id)
      (is (not (s/valid? ::sut/id id))))))

(deftest title-test
  (doseq [title ["Any" "valid string" "8. Starting from number"]]
    (testing (str "::title is valid " title)
      (is (s/valid? ::sut/title title))))

  (doseq [title [123 true "" ["list" "of" "strings"] (str/join (repeat 501 "s"))]]
    (testing (str "::title is invalid " title)
      (is (not (s/valid? ::sut/title title))))))

(deftest subtitle-test
  (doseq [subtitle ["Any" "valid string" "8. Starting from number"]]
    (testing (str "::subtitle is valid " subtitle)
      (is (s/valid? ::sut/subtitle subtitle))))

  (doseq [subtitle [123 true "" ["list" "of" "strings"] (str/join (repeat 501 "s"))]]
    (testing (str "::subtitle is invalid " subtitle)
      (is (not (s/valid? ::sut/title subtitle))))))

(deftest description-test
  (doseq [description ["any string" (str/join " " (repeat 600 "very long string"))]]
    (testing (str "::description is valid " description)
      (is (s/valid? ::sut/description description))))

  (doseq [description ["" false 1234 []]]
    (testing (str "::description is invalid " description)
      (is (not (s/valid? ::sut/description description))))))

(deftest screenshot-test
  (doseq [screenshot ["http://insecure.com"
                      "https://secure.net/some.img.jpg"
                      "https://some.url/"
                      "http://127.0.0.1:3000/some_image.jpg"
                      (str "https://some.url/" (str/join (repeat 979 "a")) ".jpg")]]
    (testing (str "::screenshot is valid " screenshot)
      (is (s/valid? ::sut/screenshot screenshot))))

  (doseq [screenshot ["juststring"
                      "www.screenshot.com/image.jpg"
                      (str "https://some.url/" (str/join (repeat 980 "a")) ".jpg")]]
    (testing (str "::screenshot is invalid " screenshot)
      (is (not (s/valid? ::sut/screenshot screenshot))))))

(deftest screenshots-test
  (testing "::screenshots is valid"
    (is (s/valid? ::sut/screenshots ["https://first.valid.screenshot" "https://second.valid.screenshot"])))

  (doseq [screenshots [#{"https://set.screenshot" "https://another.set.screenshot"}
                       ["https://first.valid.screenshot" "https://first.valid.screenshot"]]]
    (testing (str "::screenshots is invalid " screenshots)
      (is (not (s/valid? ::sut/screenshots screenshots))))))

(deftest price-test
  (doseq [price ["1" "33.3" "0.43" "43.00343434"]]
    (testing (str "::price is valid " price)
      (is (s/valid? ::sut/price price))))

  (doseq [price ["-1" "22.22.22" "string" true "111,234" "$#@#@$" [] #{}]]
    (testing (str "::price is invalid " price)
      (is (not (s/valid? ::sut/price price))))))

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

(deftest lesson-create-request-test
  (testing "::lesson-create-request is valid"
    (is (s/valid? ::sut/lesson-create-request
                  {:title       "Some title"
                   :subtitle    "Some subtitle"
                   :description "Video lesson description"
                   :screenshots []
                   :price       "7.22"}))))

(deftest lesson-update-request-test
  (doseq [request [{}
                   {:title "New title"}
                   {:subtitle "New subtitle"}
                   {:description "New description"}
                   {:screenshots ["https://alenkinaskazka.net/img/some_screenshot.jpg"]}
                   {:price "22.8383"}]]
    (testing "::lesson-update-request is valid"
      (is (s/valid? ::sut/lesson-update-request request)))))

(deftest lesson-created-response-test
  (testing "::lesson-created-response is valid"
    (is (s/valid? ::sut/lesson-created-response {:id 123})))

  (testing "::lesson-created-response is invalid"
    (is (not (s/valid? ::sut/lesson-created-response {:id "123"})))))

(deftest limit-test
  (doseq [limit [nil 999999999]]
    (testing (str "::limit is valid " limit)
      (is (s/valid? ::sut/limit limit))))

  (doseq [limit [-1 0 "string" [] #{} false true]]
    (testing (str "::limit is invalid " limit)
      (is (not (s/valid? ::sut/limit limit))))))

(deftest offset-test
  (doseq [offset [nil 999999999]]
    (testing (str "::offset is valid " offset)
      (is (s/valid? ::sut/offset offset))))

  (doseq [offset [-1 0 "string" [] #{} false true]]
    (testing (str "::offset is invalid " offset)
      (is (not (s/valid? ::sut/offset offset))))))
