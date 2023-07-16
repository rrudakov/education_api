(ns education.utils.crypt-test
  (:require
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]
   [education.test-data :as td]
   [education.utils.crypt :as sut]))

(def email
  "some@email.com")

(deftest generate-hash-test
  (testing "Test generate hash successfully for `email`"
    (let [result (sut/generate-hash td/test-config email)]
      (is (= email (sut/decrypt-hash td/test-config result))))))

(deftest decrypt-hash-test
  (testing "Test decrypt hash successfully"
    (let [hash   (sut/generate-hash td/test-config email)
          result (sut/decrypt-hash td/test-config hash)]
      (is (= email result))))

  (testing "Test decrypt bad hash"
    (let [hash (str/reverse (sut/generate-hash td/test-config email))
          result (sut/decrypt-hash td/test-config hash)]
      (is (nil? result)))))
