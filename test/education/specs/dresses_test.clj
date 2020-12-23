(ns education.specs.dresses-test
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [education.specs.dresses :as sut])
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

(deftest description-test
  (doseq [description ["any string" (str/join " " (repeat 600 "very long string"))]]
    (testing (str "::description is valid " description)
      (is (s/valid? ::sut/description description))))

  (doseq [description ["" false 1234 []]]
    (testing (str "::description is invalid " description)
      (is (not (s/valid? ::sut/description description))))))

(deftest size-test
  (doseq [size [1 99]]
    (testing (str "::size is valid " size)
      (is (s/valid? ::sut/size size))))

  (doseq [size [0 -1 "string" true #{23} [23] {:size 22}]]
    (testing (str "::size is invalid " size)
      (is (not (s/valid? ::sut/size size))))))

(deftest picture-test
  (doseq [picture ["http://insecure.com"
                   "https://secure.net/some.img.jpg"
                   "https://some.url/"
                   "http://127.0.0.1:3000/some_image.jpg"
                   (str "https://some.url/" (str/join (repeat 979 "a")) ".jpg")]]
    (testing (str "::picture is valid " picture)
      (is (s/valid? ::sut/picture picture))))

  (doseq [picture ["juststring"
                   "www.picture.com/image.jpg"
                   (str "https://some.url/" (str/join (repeat 980 "a")) ".jpg")
                   nil]]
    (testing (str "::picture is invalid " picture)
      (is (not (s/valid? ::sut/picture picture))))))

(deftest pictures-test
  (testing "::pictures is valid"
    (is (s/valid? ::sut/pictures ["https://first.valid.screenshot" "https://second.valid.screenshot"])))

  (doseq [pictures [#{"https://set.screenshot" "https://another.set.screenshot"}
                    ["https://first.valid.screenshot" "https://first.valid.screenshot"]]]
    (testing (str "::pictures is invalid " pictures)
      (is (not (s/valid? ::sut/pictures pictures))))))

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

(deftest dress-create-request-test
  (testing "::dress-create-request is valid"
    (is (s/valid? ::sut/dress-create-request
                  {:title       "Dress title"
                   :description "Dress description"
                   :size        34
                   :pictures    []
                   :price       "33.2"})))

  (doseq [request [{:description "Dress description"
                    :size        34
                    :pictures    []
                    :price       "33.2"}
                   {:title    "Dress title"
                    :size     34
                    :pictures []
                    :price    "33.2"}
                   {:title       "Dress title"
                    :description "Dress description"
                    :pictures    []
                    :price       "33.2"}
                   {:title       "Dress title"
                    :description "Dress description"
                    :size        34
                    :price       "33.2"}
                   {:title       "Dress title"
                    :description "Dress description"
                    :size        34
                    :pictures    []}]]
    (testing "::dress-create-request is invalid"
      (is (not (s/valid? ::sut/dress-create-request request))))))

(deftest dress-update-request-test
  (doseq [request [{}
                   {:title "New title"}
                   {:description "New description"}
                   {:size 88}
                   {:pictures ["https://alenkinaskazka.net/img/some_picture.jpg"]}
                   {:price "888"}]]
    (testing "::dress-update-request is valid"
      (is (s/valid? ::sut/dress-update-request request)))))

(deftest dress-create-response-test
  (testing "::dress-create-response is valid"
    (is (s/valid? ::sut/dress-create-response {:id 2})))

  (testing "::dress-create-response is invalid"
    (is (not (s/valid? ::sut/dress-create-response {:id "2"})))))

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

(deftest dress-response-test
  (doseq [response [{:id          1
                     :title       "Dress title"
                     :description "Dress description"
                     :size        22
                     :pictures    ["https://alenkinaskazka.net/img/some_picture.jpg"]
                     :price       "32.3"
                     :created_on  (Instant/now)
                     :updated_on  (Instant/now)}
                    {:id          1
                     :title       "Dress title"
                     :description "Dress description"
                     :size        22
                     :pictures    []
                     :price       "32.3"
                     :created_on  (Instant/now)
                     :updated_on  (Instant/now)}]]
    (testing "::dress-response is valid"
      (is (s/valid? ::sut/dress-response response))))

  (doseq [response [{:title       "Dress title"
                     :description "Dress description"
                     :size        22
                     :pictures    ["https://alenkinaskazka.net/img/some_picture.jpg"]
                     :price       "32.3"
                     :created_on  (Instant/now)
                     :updated_on  (Instant/now)}
                    {:id          1
                     :description "Dress description"
                     :size        22
                     :pictures    ["https://alenkinaskazka.net/img/some_picture.jpg"]
                     :price       "32.3"
                     :created_on  (Instant/now)
                     :updated_on  (Instant/now)}
                    {:id         1
                     :title      "Dress title"
                     :size       22
                     :pictures   ["https://alenkinaskazka.net/img/some_picture.jpg"]
                     :price      "32.3"
                     :created_on (Instant/now)
                     :updated_on (Instant/now)}
                    {:id          1
                     :title       "Dress title"
                     :description "Dress description"
                     :pictures    ["https://alenkinaskazka.net/img/some_picture.jpg"]
                     :price       "32.3"
                     :created_on  (Instant/now)
                     :updated_on  (Instant/now)}
                    {:id          1
                     :title       "Dress title"
                     :description "Dress description"
                     :size        22
                     :price       "32.3"
                     :created_on  (Instant/now)
                     :updated_on  (Instant/now)}
                    {:id          1
                     :title       "Dress title"
                     :description "Dress description"
                     :size        22
                     :pictures    ["https://alenkinaskazka.net/img/some_picture.jpg"]
                     :created_on  (Instant/now)
                     :updated_on  (Instant/now)}
                    {:id          1
                     :title       "Dress title"
                     :description "Dress description"
                     :size        22
                     :pictures    ["https://alenkinaskazka.net/img/some_picture.jpg"]
                     :price       "32.3"
                     :updated_on  (Instant/now)}
                    {:id          1
                     :title       "Dress title"
                     :description "Dress description"
                     :size        22
                     :pictures    ["https://alenkinaskazka.net/img/some_picture.jpg"]
                     :price       "32.3"
                     :created_on  (Instant/now)}]]
    (testing "::dress-response is invalid"
      (is (not (s/valid? ::sut/dress-response response))))))

(deftest dresses-response-test
  (testing "::dresses-response is valid"
    (is (s/valid? ::sut/dresses-response
                  [{:id          1
                    :title       "Dress title"
                    :description "Dress description"
                    :size        22
                    :pictures    ["https://alenkinaskazka.net/img/some_picture.jpg"]
                    :price       "32.3"
                    :created_on  (Instant/now)
                    :updated_on  (Instant/now)}
                   {:id          2
                    :title       "Dress title 2"
                    :description "Dress description 2"
                    :size        22
                    :pictures    ["https://alenkinaskazka.net/img/some_picture.jpg"]
                    :price       "32.3"
                    :created_on  (Instant/now)
                    :updated_on  (Instant/now)}])))

  (testing "::dresses-response is invalid"
    (let [now (Instant/now)]
      (is (not (s/valid? ::sut/dresses-response
                         [{:id          1
                           :title       "Dress title"
                           :description "Dress description"
                           :size        22
                           :pictures    ["https://alenkinaskazka.net/img/some_picture.jpg"]
                           :price       "32.3"
                           :created_on  now
                           :updated_on  now}
                          {:id          1
                           :title       "Dress title"
                           :description "Dress description"
                           :size        22
                           :pictures    ["https://alenkinaskazka.net/img/some_picture.jpg"]
                           :price       "32.3"
                           :created_on  now
                           :updated_on  now}]))))))
