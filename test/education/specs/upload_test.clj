(ns education.specs.upload-test
  (:require [education.specs.upload :as sut]
            [clojure.test :refer [deftest testing is]]
            [clojure.spec.alpha :as s]
            [clojure.java.io :as io]
            [spec-tools.core :as st]))

(deftest filename-test
  (testing "::filename is valid"
    (is (s/valid? ::sut/filename "any valid string")))

  (doseq [filename [true 123 #{"set"} ["list"]]]
    (testing (str "::filename is invalid" filename)
      (is (not (s/valid? ::sut/filename filename))))))

(deftest content-type-test
  (testing "::content-type is valid"
    (is (s/valid? ::sut/content-type "any valid string")))

  (doseq [content-type [true 123 #{"set"} ["list"] :keyword]]
    (is (not (s/valid? ::sut/content-type content-type)))))

(deftest size-test
  (testing "::size is valid"
    (is (s/valid? ::sut/size 999999999)))

  (doseq [size [0 -123 "string" 123.999 #{"set"} ["list"] :keyword]]
    (testing (str "::size is invalid " size)
      (is (not (s/valid? ::sut/size size))))))

(deftest tempfile-test
  (testing "::file is valid"
    (is (s/valid? ::sut/tempfile (io/file (io/resource "1.png")))))

  (doseq [f ["string" 123 true #{"set"} ["vec"]]]
    (testing (str "::file is invalid " f)
      (is (not (s/valid? ::sut/tempfile f))))))

(deftest file-test
  (doseq [request [{:filename     "Some file name"
                    :content-type "application/json"
                    :size         999999}
                   {:filename     "Some file name"
                    :content-type "application/json"
                    :size         1
                    :tempfile     (io/file (io/resource "1.png"))}]]
    (testing "::file is valid"
      (is (s/valid? ::sut/file request))))

  (doseq [request [{:filename     "Some file name"
                    :content-type "application/json"}
                   {:content-type "application/json"
                    :size         88}
                   {:filename "File name"
                    :size     99}
                   {:filename     "Some file name"
                    :content-type "application/json"
                    :size         1
                    :tempfile     "Something but the file"}]]
    (testing "::file is invalid"
      (is (not (s/valid? ::sut/file request)))))

  (testing "::file json-schema metadata"
    (is (= "file" (:json-schema/type (st/get-spec ::sut/file))))))

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
