(ns education.database.presentations-test
  (:require [clojure.test :refer [deftest is testing]]
            [education.database.presentations :as sut]
            [next.jdbc.sql :as sql]
            [spy.core :as spy])
  (:import java.time.Instant))

(def ^:private created-on
  "Constant value of `created_on` field."
  (Instant/now))

(def ^:private updated-on
  "Constant value of `updated_on` field."
  (Instant/now))

(def ^:private create-presentation-request
  "Test create presentation API request."
  {:title       "Presentation title"
   :url         "https://google.com/presentation/bla-bla"
   :description "This presentation is about bla-bla-bla..."})

(def ^:private create-presentation-result
  "Stub data for create presentation call."
  {:presentations/id          32
   :presentations/title       "Presentation title"
   :presentations/url         "https://google.com/presentation/bla-bla"
   :presentations/description "This presentation is about bla-bla-bla..."
   :presentations/created_on  created-on
   :presentations/updated_on  updated-on})

(deftest add-presentation-test
  (testing "Test add presentation with correct fields"
    (with-redefs [sql/insert! (spy/stub create-presentation-result)]
      (let [result (sut/add-presentation nil create-presentation-request)]
        (is (= (:presentations/id create-presentation-result) result))
        (is (spy/called-once-with? sql/insert! nil :presentations create-presentation-request))))))

(def ^:private update-presentation-id
  "Test API presentation-id."
  838)

(def ^:private update-presentation-result
  "Stub data for update presentation call."
  {:next.jdbc/update-count 1})

(deftest update-presentation-test
  (doseq [request [{:title "New title"}
                   {:url "https://google.com/presentation/new-bla-bla"}
                   {:description "Updated description"}
                   {}]]
    (testing "Test update presentation with valid fields"
      (with-redefs [sql/update! (spy/stub update-presentation-result)]
        (let [result (sut/update-presentation nil update-presentation-id request)]
          (is (= (:next.jdbc/update-count update-presentation-result) result))
          (is (spy/called-once-with? sql/update! nil :presentations request {:id update-presentation-id})))))))
