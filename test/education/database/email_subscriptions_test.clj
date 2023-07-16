(ns education.database.email-subscriptions-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [education.database.email-subscriptions :as sut]
   [next.jdbc :as jdbc]
   [next.jdbc.sql :as sql]
   [spy.core :as spy]))

(def add-email-subscription-query
  "Expected SQL query."
  "INSERT INTO email_subscriptions (email, is_active) VALUES (?, TRUE) ON CONFLICT (email) DO NOTHING")

(deftest add-email-subscription-test
  (testing "Test add email subscription successfully"
    (with-redefs [jdbc/execute-one! (spy/stub {:next.jdbc/update-count 1})]
      (let [email  "test@example.com"
            result (sut/add-email-subscription nil email)]
        (is (= {:next.jdbc/update-count 1} result))
        (is (spy/called-once-with? jdbc/execute-one! nil [add-email-subscription-query email]))))))

(def email-subscription
  "Test email subscription."
  {:email     "test@example.com"
   :is_active true})

(deftest get-email-subscription-by-email-test
  (testing "Test get email subscription by `email` successfully"
    (with-redefs [sql/get-by-id (spy/stub email-subscription)]
      (let [email  (:email email-subscription)
            result (sut/get-email-subscription-by-email nil email)]
        (is (= email-subscription result))
        (is (spy/called-once-with? sql/get-by-id nil :email_subscriptions email :email {}))))))
