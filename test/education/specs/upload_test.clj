(ns education.specs.upload-test
  (:require
   [clojure.spec.alpha :as s]
   [clojure.test :refer [deftest is testing]]
   [education.specs.upload :as sut]))

(deftest url-test
  (testing "::url is valid"
    (is (s/valid? ::sut/url "https://alenkinaskazka.net/img/some_picture.png")))

  (doseq [url ["invalid" false 222 #{"set"} ["list"]]]
    (is (not (s/valid? ::sut/url url)))))

(deftest upload-response-test
  (doseq [response [{:url "https://alenkinaskazka.net/img/some_picture.png"}
                    {:url "https://alenkinaskazka.net/img/some_picture.png" :some "extra keys"}]]
    (testing "::upload-response is valid"
      (is (s/valid? ::sut/upload-response response))))

  (doseq [response [{:url ""}
                    {:url "invalid"}
                    {:invalid "key"}
                    {}]]
    (testing "::upload-response is invalid"
      (is (not (s/valid? ::sut/upload-response response))))))
