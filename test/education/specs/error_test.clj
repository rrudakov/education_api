(ns education.specs.error-test
  (:require [clojure.test :refer [deftest is testing]]
            [education.http.constants :as const]
            [education.specs.error :as sut]
            [clojure.spec.alpha :as s]))

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
      (is (not (s/valid? ::sut/message message))))))

(deftest error-code-test
  (doseq [code ["22888" "00000" "88237" "anystring"]]
    (testing (str "::error-code valid " code)
      (is (s/valid? ::sut/error_code code))))

  (doseq [code [12345 22989 true {:some "object"} :keyword]]
    (testing (str "::error_code invalid " code)
      (is (not (s/valid? ::sut/error_code code))))))

(deftest details-test
  (testing "::details valid"
    (is (s/valid? ::sut/details "anystring")))

  (doseq [details [1234 false {:some "Object"} :keywork]]
    (testing (str "::details invalid " details)
      (is (not (s/valid? ::sut/details details))))))

(deftest error-response-test
  (doseq [resp [{:message const/bad-request-error-message}
                {:message const/conflict-error-message :error_code "22023"}
                {:message const/invalid-credentials-error-message :details "Some error details"}
                {:message const/no-access-error-message :error_code "99999" :details "Any string"}
                {:message const/not-authorized-error-message :any "ignored"}]]
    (testing (str "::error-response valid " resp)
      (is (s/valid? ::sut/error-response resp))))

  (doseq [resp [{:message {:value "Some message"}}
                {:details "23456" :error_code "error code"}
                {:message const/server-error-message :details :invalid :error_code "valid"}
                {:message const/not-authorized-error-message :details "valid" :error_code 12345}]]
    (testing (str "::error-response invalid " resp)
      (is (not (s/valid? ::sut/error-response resp))))))
