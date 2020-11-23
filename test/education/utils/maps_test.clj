(ns education.utils.maps-test
  (:require [education.utils.maps :as sut]
            [clojure.test :refer [testing deftest is]]))

(deftest unqualify-keywork-test
  (testing "Test unqualify keyword with namespace"
    (is (= [:id 1] (sut/unqualify-keyword [:lessons/id 1]))))

  (testing "Test unqualify keyword without namespace"
    (is (= [:title "Some title"] (sut/unqualify-keyword [:title "Some title"])))))

(deftest unqualify-map-test
  (testing "Test unqualify map with namespaces"
    (is (= {:id       1
            :title    "Some title"
            :subtitle "Some subtitle"}
           (sut/unqualify-map {:lessons/id       1
                               :lessons/title    "Some title"
                               :lessons/subtitle "Some subtitle"}))))

  (testing "Test unqualify map without namespaces"
    (is (= {:id       1
            :title    "Some title"
            :subtitle "Some subtitle"}
           (sut/unqualify-map {:id       1
                               :title    "Some title"
                               :subtitle "Some subtitle"})))))

(deftest update-if-exist-test
  (testing "Test update existing key"
    (is (= {:id 2 :title "Some title"}
           (sut/update-if-exist {:id 1 :title "Some title"} :id inc))))

  (testing "Test update non-existing key"
    (is (= {:id 1 :title "Some title"}
           (sut/update-if-exist {:id 1 :title "Some title"} :extra inc)))))
