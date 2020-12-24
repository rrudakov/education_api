(ns education.specs.gymnastics-test
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [education.specs.gymnastics :as sut])
  (:import java.time.Instant))

(deftest id-test
  (doseq [id [1 33 9999999999992222]]
    (testing (str "::id is valid " id)
      (is (s/valid? ::sut/id id))))

  (doseq [id ["string" false -1]]
    (testing (str "::id is invalid " id)
      (is (not (s/valid? ::sut/id id))))))

(deftest subtype-id-test
  (doseq [subtype-id [1 33 9999999999992222]]
    (testing (str "::subtype-id is valid " subtype-id)
      (is (s/valid? ::sut/subtype_id subtype-id))))

  (doseq [subtype-id ["string" false -1]]
    (testing (str "::subtype-id is invalid " subtype-id)
      (is (not (s/valid? ::sut/subtype_id subtype-id))))))

(deftest title-test
  (doseq [title ["Any" "valid string" "8. Starting from number"]]
    (testing (str "::title is valid " title)
      (is (s/valid? ::sut/title title))))

  (doseq [title [123 true "" ["list" "of" "strings"] (str/join (repeat 501 "s"))]]
    (testing (str "::title is invalid " title)
      (is (not (s/valid? ::sut/title title))))))

(deftest description-test
  (doseq [description ["any string" (str/join " " (repeat 600 "very long string"))]]
    (testing (str "::description is valid " description)
      (is (s/valid? ::sut/description description))))

  (doseq [description ["" false 1234 []]]
    (testing (str "::description is invalid " description)
      (is (not (s/valid? ::sut/description description))))))

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

(deftest limit-test
  (doseq [limit [1 999999999]]
    (testing (str "::limit is valid " limit)
      (is (s/valid? ::sut/limit limit))))

  (doseq [limit [-1 0 nil "string" [] #{} false true]]
    (testing (str "::limit is invalid " limit)
      (is (not (s/valid? ::sut/limit limit))))))

(deftest offset-test
  (doseq [offset [1 999999999]]
    (testing (str "::offset is valid " offset)
      (is (s/valid? ::sut/offset offset))))

  (doseq [offset [-1 0 nil "string" [] #{} false true]]
    (testing (str "::offset is invalid " offset)
      (is (not (s/valid? ::sut/offset offset))))))

(deftest gymnastic-create-request-test
  (doseq [request [{:subtype_id  2
                    :title       "Gymnastic title"
                    :description "Gymnastic description"}
                   {:subtype_id  2
                    :title       "Gymnastic title"
                    :description "Gymnastic description"
                    :picture     nil}
                   {:subtype_id  8
                    :title       "Gymnastic title"
                    :description "Gymnastic description"
                    :picture     "https://alenkinaskazka.net/valid/picture.png"}]]
    (testing "::gymnastic-create-request is valid"
      (is (s/valid? ::sut/gymnastic-create-request request))))

  (doseq [request [{:title       "Gymnastic title"
                    :description "Gymnastic description"
                    :picture     "https://alenkinaskazka.net/valid/picture.png"}
                   {:subtype_id  8
                    :description "Gymnastic description"
                    :picture     "https://alenkinaskazka.net/valid/picture.png"}
                   {:subtype_id 8
                    :title      "Gymnastic title"
                    :picture    "https://alenkinaskazka.net/valid/picture.png"}]]
    (testing "::gymnastic-create-request is invalid"
      (is (not (s/valid? ::sut/gymnastic-create-request request))))))

(deftest gymnastic-update-request-test
  (doseq [request [{}
                   {:subtype_id 88}
                   {:title "New title"}
                   {:description "New description"}
                   {:picture nil}
                   {:picture "https://alenkinaskazka.net/new/picture.png"}]]
    (testing "::gymnastic-update-request is valid"
      (is (s/valid? ::sut/gymnastic-update-request request)))))

(deftest gymnastic-create-response-test
  (testing "::gymnastic-create-response is valid"
    (is (s/valid? ::sut/gymnastic-create-response {:id 2})))

  (testing "::gymnastic-create-response is invalid"
    (is (not (s/valid? ::sut/gymnastic-create-response {:id "2"})))))

(deftest gymnastic-response-test
  (doseq [response [{:id          1
                     :subtype_id  2
                     :title       "Gymnastic title"
                     :description "Gymnastic description"
                     :picture     "https://alenkinaskazka.net/img/picture.png"
                     :created_on  (Instant/now)
                     :updated_on  (Instant/now)}
                    {:id          1
                     :subtype_id  2
                     :title       "Gymnastic title"
                     :description "Gymnastic description"
                     :picture     nil
                     :created_on  (Instant/now)
                     :updated_on  (Instant/now)}
                    {:id          1
                     :subtype_id  2
                     :title       "Gymnastic title"
                     :description "Gymnastic description"
                     :created_on  (Instant/now)
                     :updated_on  (Instant/now)}]]
    (testing "::gymnastic-response is valid"
      (is (s/valid? ::sut/gymnastic-response response))))

  (doseq [response [{:subtype_id  2
                     :title       "Gymnastic title"
                     :description "Gymnastic description"
                     :picture     "https://alenkinaskazka.net/img/picture.png"
                     :created_on  (Instant/now)
                     :updated_on  (Instant/now)}
                    {:id          1
                     :title       "Gymnastic title"
                     :description "Gymnastic description"
                     :picture     "https://alenkinaskazka.net/img/picture.png"
                     :created_on  (Instant/now)
                     :updated_on  (Instant/now)}
                    {:id          1
                     :subtype_id  2
                     :description "Gymnastic description"
                     :picture     "https://alenkinaskazka.net/img/picture.png"
                     :created_on  (Instant/now)
                     :updated_on  (Instant/now)}
                    {:id         1
                     :subtype_id 2
                     :title      "Gymnastic title"
                     :picture    "https://alenkinaskazka.net/img/picture.png"
                     :created_on (Instant/now)
                     :updated_on (Instant/now)}
                    {:id          1
                     :subtype_id  2
                     :title       "Gymnastic title"
                     :description "Gymnastic description"
                     :picture     "https://alenkinaskazka.net/img/picture.png"
                     :updated_on  (Instant/now)}
                    {:id          1
                     :subtype_id  2
                     :title       "Gymnastic title"
                     :description "Gymnastic description"
                     :picture     "https://alenkinaskazka.net/img/picture.png"
                     :created_on  (Instant/now)}]]
    (testing "::gymnastic-response is invalid"
      (is (not (s/valid? ::sut/gymnastic-response response))))))

(deftest gymnastics-response-test
  (testing "::gymnastics-response is valid"
    (is (s/valid? ::sut/gymnastics-response
                  [{:id          1
                    :subtype_id  2
                    :title       "Gymnastic title"
                    :description "Gymnastic description"
                    :picture     "https://alenkinaskazka.net/img/picture.png"
                    :created_on  (Instant/now)
                    :updated_on  (Instant/now)}
                   {:id          2
                    :subtype_id  2
                    :title       "Gymnastic title 2"
                    :description "Gymnastic description 2"
                    :picture     "https://alenkinaskazka.net/img/picture.png"
                    :created_on  (Instant/now)
                    :updated_on  (Instant/now)}])))

  (testing "::gymnastics-response is invalid"
    (let [now (Instant/now)]
      (is (not (s/valid? ::sut/gymnastics-response
                         [{:id          1
                           :subtype_id  2
                           :title       "Gymnastic title"
                           :description "Gymnastic description"
                           :picture     "https://alenkinaskazka.net/img/picture.png"
                           :created_on  now
                           :updated_on  now}
                          {:id          1
                           :subtype_id  2
                           :title       "Gymnastic title"
                           :description "Gymnastic description"
                           :picture     "https://alenkinaskazka.net/img/picture.png"
                           :created_on  now
                           :updated_on  now}]))))))
