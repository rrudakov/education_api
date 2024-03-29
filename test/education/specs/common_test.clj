(ns education.specs.common-test
  (:require [cljc.java-time.instant :as instant]
            [clojure.spec.alpha :as s]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [education.http.constants :as const]
            [education.specs.common :as sut]))

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

(deftest size-test
  (doseq [size [1 99]]
    (testing (str "::size is valid " size)
      (is (s/valid? ::sut/size size))))

  (doseq [size [0 -1 "string" true #{23} [23] {:size 22}]]
    (testing (str "::size is invalid " size)
      (is (not (s/valid? ::sut/size size))))))

(deftest price-test
  (doseq [price ["1" "33.3" "0.43" "43.00343434"]]
    (testing (str "::price is valid " price)
      (is (s/valid? ::sut/price price))))

  (doseq [price ["-1" "22.22.22" "string" true "111,234" "$#@#@$" [] #{}]]
    (testing (str "::price is invalid " price)
      (is (not (s/valid? ::sut/price price))))))

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

(deftest attachment-test
  (doseq [attachment ["http://insecure.com"
                      "https://secure.net/some.img.jpg"
                      "https://some.url/"
                      "http://127.0.0.1:3000/some_image.jpg"
                      (str "https://some.url/" (str/join (repeat 979 "a")) ".jpg")]]
    (testing (str "::attachment is valid " attachment)
      (is (s/valid? ::sut/attachment attachment))))

  (doseq [attachment ["juststring"
                      "www.attachment.com/image.jpg"
                      (str "https://some.url/" (str/join (repeat 980 "a")) ".jpg")]]
    (testing (str "::attachment is invalid " attachment)
      (is (not (s/valid? ::sut/attachment attachment))))))

(deftest preview-test
  (doseq [preview ["http://insecure.com"
                   "https://secure.net/some.img.jpg"
                   "https://some.url/"
                   "http://127.0.0.1:3000/some_image.jpg"
                   (str "https://some.url/" (str/join (repeat 979 "a")) ".jpg")]]
    (testing (str "::preview is valid " preview)
      (is (s/valid? ::sut/preview preview))))

  (doseq [preview ["juststring"
                   "www.preview.com/image.jpg"
                   (str "https://some.url/" (str/join (repeat 980 "a")) ".jpg")
                   nil]]
    (testing (str "::preview is invalid " preview)
      (is (not (s/valid? ::sut/preview preview))))))

(deftest is-public-test
  (doseq [is-public [true false]]
    (testing (str "::is-public is valid " is-public)
      (is (s/valid? ::sut/is_public is-public))))

  (doseq [is-public ["string" 123 nil [] #{} :keyword]]
    (testing (str "::is-public is invalid")
      (is ((complement s/valid?) ::sut/is_public is-public)))))

(deftest created-on-test
  (testing "::created_on is valid"
    (is (s/valid? ::sut/created_on (instant/now))))

  (doseq [created-on ["2020-06-23T18:05:22.905106Z" "any_value" 1234567888 false]]
    (testing (str "::created_on is invalid " created-on)
      (is (not (s/valid? ::sut/created_on created-on))))))

(deftest updated-on-test
  (testing "::updated_on is valid"
    (is (s/valid? ::sut/updated_on (instant/now))))

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

(deftest create-response-test
  (testing "::gymnastic-create-response is valid"
    (is (s/valid? ::sut/create-response {:id 2})))

  (doseq [response ["string"
                    123
                    true
                    ["vector"]
                    #{:set}
                    {}
                    {:wrong 3}
                    {:id "2"}]]
    (testing "::gymnastic-create-response is invalid"
      (is (not (s/valid? ::sut/create-response response))))))

(def ^:private gymnastic-create-request
  {:subtype_id  8
   :title       "Gymnastic title"
   :description "Gymnastic description"
   :picture     "https://alenkinaskazka.net/valid/picture.png"})

(deftest gymnastic-create-request-test
  (doseq [request [gymnastic-create-request
                   (assoc gymnastic-create-request :picture nil)
                   (dissoc gymnastic-create-request :picture)]]
    (testing "::gymnastic-create-request is valid"
      (is (s/valid? ::sut/gymnastic-create-request request))))

  (doseq [request ["string"
                   123
                   true
                   ["vector"]
                   #{:set}
                   (dissoc gymnastic-create-request :subtype_id)
                   (dissoc gymnastic-create-request :title)
                   (dissoc gymnastic-create-request :description)]]
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
      (is (s/valid? ::sut/gymnastic-update-request request))))

  (doseq [request ["string"
                   123
                   true
                   ["vector"]
                   #{:set}]]
    (testing "::gymnastic-update-request is invalid"
      (is ((complement s/valid?) ::sut/gymnastic-update-request request)))))

(def ^:private gymnastic-response
  {:id          1
   :subtype_id  2
   :title       "Gymnastic title"
   :description "Gymnastic description"
   :picture     "https://alenkinaskazka.net/img/picture.png"
   :created_on  (instant/now)
   :updated_on  (instant/now)})

(def ^:private gymnastic-response2
  {:id          2
   :subtype_id  2
   :title       "Gymnastic title 2"
   :description "Gymnastic description 2"
   :picture     "https://alenkinaskazka.net/img/picture.png"
   :created_on  (instant/now)
   :updated_on  (instant/now)})

(deftest gymnastic-response-test
  (doseq [response [gymnastic-response
                    (assoc gymnastic-response :picture nil)
                    (dissoc gymnastic-response :picture)]]
    (testing "::gymnastic-response is valid"
      (is (s/valid? ::sut/gymnastic-response response))))

  (doseq [response ["string"
                    123
                    true
                    ["vector"]
                    #{:set}(dissoc gymnastic-response :id)
                    (dissoc gymnastic-response :subtype_id)
                    (dissoc gymnastic-response :title)
                    (dissoc gymnastic-response :description)
                    (dissoc gymnastic-response :created_on)
                    (dissoc gymnastic-response :updated_on)]]
    (testing "::gymnastic-response is invalid"
      (is (not (s/valid? ::sut/gymnastic-response response))))))

(deftest gymnastics-response-test
  (testing "::gymnastics-response is valid"
    (is (s/valid? ::sut/gymnastics-response [gymnastic-response gymnastic-response2])))

  (doseq [response ["string"
                    123
                    true
                    ["vector"]
                    #{:set}
                    [gymnastic-response gymnastic-response]]]
    (testing "::gymnastics-response is invalid"
      (is ((complement s/valid?) ::sut/gymnastics-response response)))))

(def ^:private dress-create-request
  {:title       "Dress title"
   :description "Dress description"
   :size        34
   :pictures    []
   :price       "33.2"})

(deftest dress-create-request-test
  (testing "::dress-create-request is valid"
    (is (s/valid? ::sut/dress-create-request dress-create-request)))

  (doseq [request ["string"
                   123
                   true
                   ["vector"]
                   #{:set}
                   {}
                   (dissoc dress-create-request :title)
                   (dissoc dress-create-request :description)
                   (dissoc dress-create-request :size)
                   (dissoc dress-create-request :pictures)
                   (dissoc dress-create-request :price)]]
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
      (is (s/valid? ::sut/dress-update-request request))))

  (doseq [request ["string"
                   123
                   true
                   ["vector"]
                   #{:set}]]
    (testing "::dress-update-request is invalid"
      (is ((complement s/valid?) ::sut/dress-update-request request)))))

(def ^:private dress-response
  {:id          1
   :title       "Dress title"
   :description "Dress description"
   :size        22
   :pictures    ["https://alenkinaskazka.net/img/some_picture.jpg"]
   :price       "32.3"
   :created_on  (instant/now)
   :updated_on  (instant/now)})

(def ^:private dress-response2
  {:id          2
   :title       "Dress title 2"
   :description "Dress description 2"
   :size        22
   :pictures    ["https://alenkinaskazka.net/img/some_picture.jpg"]
   :price       "32.3"
   :created_on  (instant/now)
   :updated_on  (instant/now)})

(deftest dress-response-test
  (doseq [response [dress-response
                    (assoc dress-response :pictures [])]]
    (testing "::dress-response is valid"
      (is (s/valid? ::sut/dress-response response))))

  (doseq [response ["string"
                    123
                    true
                    ["vector"]
                    #{:set}
                    {}
                    (dissoc dress-response :id)
                    (dissoc dress-response :title)
                    (dissoc dress-response :description)
                    (dissoc dress-response :size)
                    (dissoc dress-response :pictures)
                    (dissoc dress-response :price)
                    (dissoc dress-response :created_on)
                    (dissoc dress-response :updated_on)]]
    (testing "::dress-response is invalid"
      (is ((complement s/valid?) ::sut/dress-response response)))))

(deftest dresses-response-test
  (testing "::dresses-response is valid"
    (is (s/valid? ::sut/dresses-response
                  [dress-response dress-response2])))

  (doseq [response ["string"
                    123
                    true
                    ["vector"]
                    #{:set}
                    {}
                    [dress-response dress-response]]]
    (testing "::dresses-response is invalid"
      (is ((complement s/valid?) ::sut/dresses-response response)))))

(deftest message-test
  (doseq [message [const/bad-request-error-message
                   const/conflict-error-message
                   const/invalid-credentials-error-message
                   const/no-access-error-message
                   const/not-authorized-error-message
                   const/not-found-error-message
                   const/server-error-message]]
    (testing (str "::message valid " message)
      (is (s/valid? ::sut/message message))))

  (doseq [message [123 true :keyword {:some "Object"}]]
    (testing (str "::message invalid " message)
      (is ((complement s/valid?) ::sut/message message)))))

(deftest error-code-test
  (doseq [code ["22888" "00000" "88237" "anystring"]]
    (testing (str "::error-code valid " code)
      (is (s/valid? ::sut/error_code code))))

  (doseq [code [12345 22989 true {:some "object"} :keyword]]
    (testing (str "::error_code invalid " code)
      (is ((complement s/valid?) ::sut/error_code code)))))

(deftest details-test
  (testing "::details valid"
    (is (s/valid? ::sut/details "anystring")))

  (doseq [details [1234 false {:some "Object"} :keywork]]
    (testing (str "::details invalid " details)
      (is ((complement s/valid?) ::sut/details details)))))

(deftest error-response-test
  (doseq [resp [{:message const/bad-request-error-message}
                {:message const/conflict-error-message :error_code "22023"}
                {:message const/invalid-credentials-error-message :details "Some error details"}
                {:message const/no-access-error-message :error_code "99999" :details "Any string"}
                {:message const/not-authorized-error-message :any "ignored"}]]
    (testing (str "::error-response valid " resp)
      (is (s/valid? ::sut/error-response resp))))

  (doseq [resp ["string"
                123
                true
                ["vector"]
                #{:set}
                {:message {:value "Some message"}}
                {:details "23456" :error_code "error code"}
                {:message const/server-error-message :details :invalid :error_code "valid"}
                {:message const/not-authorized-error-message :details "valid" :error_code 12345}]]
    (testing (str "::error-response invalid " resp)
      (is ((complement s/valid?) ::sut/error-response resp)))))

(def ^:private lesson-create-request
  {:title       "Some title"
   :subtitle    "Some subtitle"
   :description "Video lesson description"
   :screenshots ["https://alenkinaskazka.net/img/some_screenshot.jpg"]
   :price       "7.22"})

(deftest lesson-create-request-test
  (testing "::lesson-create-request is valid"
    (is (s/valid? ::sut/lesson-create-request lesson-create-request)))

  (doseq [request ["string"
                   123
                   true
                   ["vector"]
                   #{:set}
                   {}
                   (dissoc lesson-create-request :title)
                   (dissoc lesson-create-request :subtitle)
                   (dissoc lesson-create-request :description)
                   (dissoc lesson-create-request :screenshots)
                   (dissoc lesson-create-request :price)]]
    (testing "::lesson-create-request is invalid"
      (is (not (s/valid? ::sut/lesson-create-request request))))))

(deftest lesson-update-request-test
  (doseq [request [{}
                   {:title "New title"}
                   {:subtitle "New subtitle"}
                   {:description "New description"}
                   {:screenshots ["https://alenkinaskazka.net/img/some_screenshot.jpg"]}
                   {:price "22.8383"}]]
    (testing "::lesson-update-request is valid"
      (is (s/valid? ::sut/lesson-update-request request))))

  (doseq [request ["string"
                   123
                   true
                   ["vector"]
                   #{:set}]]
    (testing "::lesson-update-request is not valid"
      (is ((complement s/valid?) ::sut/lesson-update-request request)))))

(def ^:private lesson-response
  {:id          1
   :title       "Lesson title"
   :subtitle    "Lesson subtitle"
   :description "Lesson description"
   :screenshots ["https://alenkinaskazka.net/img/some_picture.jpg"
                 "https://alenkinaskazka.net/img/some_screenshot.jpg"]
   :price       "22.33"
   :created_on  (instant/now)
   :updated_on  (instant/now)})

(def ^:private lesson-response2
  {:id          2
   :title       "Lesson title 2"
   :subtitle    "Lesson subtitle 2"
   :description "Lesson description 2"
   :screenshots ["https://alenkinaskazka.net/img/some_more_picture.jpg"
                 "https://alenkinaskazka.net/img/some_more_screenshot.jpg"]
   :price       "22.33"
   :created_on  (instant/now)
   :updated_on  (instant/now)})

(deftest lesson-response-test
  (doseq [response [lesson-response
                    (assoc lesson-response :screenshots [])]]
    (testing "::lesson-response is valid"
      (is (s/valid? ::sut/lesson-response response))))

  (doseq [response ["string"
                    123
                    true
                    ["vector"]
                    #{:set}
                    {}
                    (dissoc lesson-response :id)
                    (dissoc lesson-response :title)
                    (dissoc lesson-response :subtitle)
                    (dissoc lesson-response :description)
                    (dissoc lesson-response :screenshots)
                    (dissoc lesson-response :price)
                    (dissoc lesson-response :created_on)
                    (dissoc lesson-response :updated_on)]]
    (testing "::lesson-response is invalid"
      (is ((complement s/valid?) ::sut/lesson-response response)))))

(deftest lessons-response-test
  (testing "::lessons-response is valid"
    (is (s/valid? ::sut/lessons-response
                  [lesson-response lesson-response2])))

  (doseq [response ["string"
                    123
                    true
                    {}
                    ["vector"]
                    #{:set}
                    [lesson-response lesson-response]]]
    (testing "::lessons-response is invalid"
      (is ((complement s/valid?) ::sut/lessons-response response)))))

(def ^:private presentation-create-request
  {:title       "My awesome presentation"
   :url         "https://google.com/api/presentations/bla-bla-bla"
   :description "This presentations is about bla-bla-bla"
   :is_public   false
   :attachment  "https://alenkinaskazka.net/some_file.pdf"
   :preview     "https://alenkinaskazka.net/some_image.png"})

(deftest presentation-create-request-test
  (doseq [request [presentation-create-request
                   (dissoc presentation-create-request :is_public)
                   (dissoc presentation-create-request :attachment)
                   (dissoc presentation-create-request :preview)]]
    (testing "::presentations-create-request is valid"
      (is (s/valid? ::sut/presentation-create-request request))))

  (doseq [request ["string"
                   123
                   true
                   ["vector"]
                   #{:set}
                   {}
                   (dissoc presentation-create-request :title)
                   (dissoc presentation-create-request :url)
                   (dissoc presentation-create-request :description)]]
    (testing "::presentation-create-request is invalid"
      (is ((complement s/valid?) ::sut/presentation-create-request request)))))

(deftest presentations-update-request-test
  (doseq [request (conj
                   (for [k (keys presentation-create-request)]
                     (select-keys presentation-create-request [k]))
                   {})]
    (testing "::presentation-update-request is valid"
      (is (s/valid? ::sut/presentation-update-request request))))

  (doseq [request ["string"
                   123
                   true
                   ["vector"]
                   #{:set}]]
    (testing "::presentation-update-request is not valid"
      (is ((complement s/valid?) ::sut/presentation-update-request request)))))

(def ^:private presentation-response
  {:id          1
   :title       "First title"
   :url         "https://google.com/first/presentation"
   :description "First presentation description"
   :is_public   false
   :subtype_id  2
   :created_on  (instant/now)
   :updated_on  (instant/now)})

(def ^:private presentation-response2
  {:id          2
   :title       "Second title"
   :description "Second presentation description"
   :is_public   false
   :attachment  "https://alenkinaskazka.net/some-file.pdf"
   :preview     "https://alenkinaskazka.net/some_image.png"
   :subtype_id  1
   :created_on  (instant/now)
   :updated_on  (instant/now)})

(deftest presentation-response-test
  (doseq [response [presentation-response
                    presentation-response2]]
    (testing "::presentation-response is valid"
      (is (s/valid? ::sut/presentation-response response))))

  (doseq [response (-> (for [k     (keys presentation-response)
                             :when (not= k :url)]
                         (dissoc presentation-response k))
                       (conj "string")
                       (conj 123)
                       (conj true)
                       (conj ["vector"])
                       (conj #{:set})
                       (conj {}))]
    (is ((complement s/valid?) ::sut/presentation-response response))))

(deftest presentations-response-test
  (testing "::presentations-response is valid"
    (is (s/valid? ::sut/presentations-response
                  [presentation-response presentation-response2])))

  (doseq [response ["string"
                    123
                    true
                    ["vector"]
                    #{:set}
                    [presentation-response presentation-response]
                    #{presentation-response presentation-response2}]]
    (testing "::presentations-response is invalid"
      (is ((complement s/valid?) ::sut/presentations-response response)))))

(def ^:private material-create-request
  {:title       "My awesome material"
   :description "This material is for bla-bla-bla"
   :preview     "https://alenkinaskazka.nl/api/materials/bla-bla-bla.png"
   :store_link  "https://some.store/link"
   :price       "99"})

(deftest material-create-request-test
  (testing "::material-create-request is valid"
    (is (s/valid? ::sut/material-create-request material-create-request)))

  (doseq [request (-> (for [k (keys material-create-request)]
                        (dissoc material-create-request k))
                      (conj "string")
                      (conj 123)
                      (conj true)
                      (conj ["vector"])
                      (conj #{:set})
                      (conj {}))]
    (testing "::material-create-request is invalid"
      (is ((complement s/valid?) ::sut/material-create-request request)))))

(deftest material-update-request-test
  (doseq [request (-> (for [k (keys material-create-request)]
                        (select-keys material-create-request [k]))
                      (conj {}))]
    (testing "::material-update-request is valid"
      (is (s/valid? ::sut/material-update-request request))))

  (doseq [request ["string"
                   123
                   true
                   ["vector"]
                   #{:set}]]
    (testing "::material-update-request is not valid"
      (is ((complement s/valid?) ::sut/material-update-request request)))))

(def ^:private material-response
  {:id          (inc (rand-int 100))
   :title       "Title"
   :description "Description"
   :preview     "https://alenkinaskazka.nl/img/some.png"
   :store_link  "https://some.store/link"
   :price       "3.99"
   :created_on  (instant/now)
   :updated_on  (instant/now)})

(def ^:private material-response-2
  {:id          (inc (rand-int 100))
   :title       "Title 2"
   :description "description 2"
   :preview     "https://alenkinaskazka.nl/img/another.png"
   :store_link  "https://some.store/another/link"
   :price       "2.99"
   :created_on  (instant/now)
   :updated_on  (instant/now)})

(deftest material-response-test
  (testing "::material-response is valid"
    (is (s/valid? ::sut/material-response material-response)))

  (doseq [response (-> (for [k (keys material-response)]
                         (dissoc material-response k))
                       (conj "string")
                       (conj 123)
                       (conj true)
                       (conj ["vector"])
                       (conj #{:set})
                       (conj {}))]
    (testing "::material-response is not valid"
      (is ((complement s/valid?) ::sut/materials-response response)))))

(deftest materials-response-test
  (testing "::materials-response is valid"
    (is (s/valid? ::sut/materials-response [material-response material-response-2])))

  (doseq [response ["string"
                    123
                    true
                    ["vector"]
                    #{:set}
                    [material-response material-response]
                    #{material-response material-response-2}]]
    (testing "::materials-response is not valid"
      (is ((complement s/valid?) ::sut/materials-response response)))))
