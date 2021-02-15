(ns education.http.constants-test
  (:require [clojure.test :refer [deftest is testing]]
            [education.http.constants :as sut]
            [education.specs.common :as spec]
            [education.specs.users :as user-spec]
            [clojure.string :as str]))

(def test-data
  "Test cases."
  [[["Field url is mandatory"]
    ::spec/presentation-create-request
    {:title       "Title"
     :description "Description"}]

   [["Username must start from letter"]
    ::user-spec/username
    "1asdf"]

   [["Username must start from letter"]
    ::user-spec/user-create-request
    {:username "1asdf"
     :password "123456"
     :email    "email@example.com"}]

   [["Username must be at least 2 characters"]
    ::user-spec/username
    "a"]

   [["Username must be at least 2 characters"]
    ::user-spec/user-create-request
    {:username "a"
     :password "123456"
     :email    "email@example.com"}]

   [["Title must not be empty"]
    ::spec/title
    ""]

   [["Title must not be empty"]
    ::spec/presentation-create-request
    {:title       ""
     :url         "https://google.com/api/presentation"
     :description "Description"}]

   [["Title must not be longer than 500 characters"]
    ::spec/title
    (str/join (repeat 501 "a"))]

   [["Title must not be longer than 500 characters"]
    ::spec/presentation-create-request
    {:title       (str/join (repeat 501 "a"))
     :url         "https://google.com/api/presentation"
     :description "Description"}]

   [["Url URL is not valid"]
    ::spec/url
    "invalid"]

   [["Url URL is not valid"]
    ::spec/presentation-create-request
    {:title       "Title"
     :url         "invalid"
     :description "Description"}]

   [["Price is not valid"]
    ::spec/price
    "invalid"]

   [["Price is not valid"]
    ::spec/dress-create-request
    {:title       "title"
     :description "description"
     :pictures    []
     :size        22
     :price       "invalid"}]

   [["Email is not valid"]
    ::user-spec/email
    "invalid@email"]

   [["Email is not valid"]
    ::user-spec/user-create-request
    {:username "username"
     :password "123456"
     :email    "invalid@email"}]

   [["Id is not valid"]
    ::spec/id
    "invalid"]

   [["Username is not valid"]
    ::user-spec/user-create-request
    {:username 123
     :password "123456"
     :email    "valid@email.com"}]

   [["Field description is mandatory"
     "Title must not be longer than 500 characters"]
    ::spec/presentation-create-request
    {:title (str/join (repeat 501 "a"))
     :url   "https://google.com/api/presentation"}]])

(deftest ->phrases-test
  (doseq [[errors spec val] test-data]
    (testing "Test conversion error to phrase"
      (is (= errors (sut/->phrases spec val))))))
