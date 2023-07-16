(ns education.utils.maps-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [education.utils.maps :as sut]))

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

(deftest ->camel-key-test
  (testing "Test ->camel-key with namespace"
    (is (= [:createdOn 123] (sut/->camel-key [:dresses/created-on 123])))
    (is (= [:createdOn 123] (sut/->camel-key [:dresses/created_on 123]))))

  (testing "Test ->camel-key without namespace"
    (is (= [:updatedOn 222] (sut/->camel-key [:gymnastics/updated-on 222])))
    (is (= [:updatedOn 222] (sut/->camel-key [:gymnastics/updated_on 222])))))

(deftest ->camel-case-test
  (testing "Test ->camel-case with namespaces"
    (is (= {:id        1
            :someTitle "Some title"
            :subTitle  "Subtitle"
            :newField  33}
           (sut/->camel-case {:dresses/id         1
                              :dresses/some_title "Some title"
                              :dresses/sub-title  "Subtitle"
                              :dresses/newField   33}))))

  (testing "Test ->camel-case without namespaces"
    (is (= {:id        1
            :someTitle "Some title"
            :subTitle  "Subtitle"
            :newField  33}
           (sut/->camel-case {:id         1
                              :some_title "Some title"
                              :sub-title  "Subtitle"
                              :newField   33})))))

(deftest remove-nils-test
  (testing "Test remove-nils"
    (is (= {:id 1 :some_title "Title"}
           (sut/remove-nils {:id 1 :some_title "Title" :attachment nil})))))
