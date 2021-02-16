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

(def ^:private create-presentation-request-v2
  "Test create presentation API v2 request."
  {:title       "Presentation title"
   :url         "https://google.com/presentation/bla-bla"
   :description "This presentation is about bla-bla-bla..."
   :is_public   false
   :attachment  "https://alenkinaskazka.net/some_file.pdf"})

(def ^:private create-presentation-result
  "Stub data for create presentation call."
  {:presentations/id          32
   :presentations/title       "Presentation title"
   :presentations/url         "https://google.com/presentation/bla-bla"
   :presentations/description "This presentation is about bla-bla-bla..."
   :presentations/is_public   true
   :presentations/attachment  nil
   :presentations/created_on  created-on
   :presentations/updated_on  updated-on})

(def ^:private create-presentation-result-not-public
  {:presentations/id          33
   :presentations/title       "Presentation title"
   :presentations/url         "https://google.com/presentation/bla-bla"
   :presentations/description "This presentation is about bla-bla-bla..."
   :presentations/is_public   false
   :presentations/attachment  "https://alenkinaskazka.net/some_file.pdf"
   :presentations/created_on  created-on
   :presentations/updated_on  updated-on})

(deftest add-presentation-test
  (testing "Test add presentation with correct fields"
    (with-redefs [sql/insert! (spy/stub create-presentation-result)]
      (let [result (sut/add-presentation nil create-presentation-request)]
        (is (= (:presentations/id create-presentation-result) result))
        (is (spy/called-once-with? sql/insert! nil :presentations
                                   (assoc create-presentation-request :is_public true))))))

  (testing "Test add presentation for v2 API request"
    (with-redefs [sql/insert! (spy/stub create-presentation-result)]
      (let  [result (sut/add-presentation nil create-presentation-request-v2)]
        (is (= (:presentations/id create-presentation-result) result))
        (is (spy/called-once-with? sql/insert! nil :presentations create-presentation-request-v2))))))

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
                   {:is_public false}
                   {:attachment "https://alenkinaskazka.net/some_file.pdf"}
                   {}]]
    (testing "Test update presentation with valid fields"
      (with-redefs [sql/update! (spy/stub update-presentation-result)]
        (let [result (sut/update-presentation nil update-presentation-id request)]
          (is (= (:next.jdbc/update-count update-presentation-result) result))
          (is (spy/called-once-with? sql/update! nil :presentations request {:id update-presentation-id})))))))

(def ^:private get-presentation-id
  "Test API presentation-id."
  838)

(deftest get-presentation-by-id-test
  (testing "Test get presentation by ID with valid `presentation-id`"
    (with-redefs [sql/get-by-id (spy/stub create-presentation-result)]
      (let [result (sut/get-presentation-by-id nil get-presentation-id)]
        (is (= create-presentation-result result))
        (is (spy/called-once-with? sql/get-by-id nil :presentations get-presentation-id))))))

(def ^:private get-all-presentations-query
  "Expected SQL query to get all presentations."
  "SELECT * FROM presentations ORDER BY updated_on DESC LIMIT ? OFFSET ?")

(deftest get-all-presentations-test
  (testing "Test get all presentations without optional parameters"
    (with-redefs [sql/query (spy/stub [create-presentation-result])]
      (let [result (sut/get-all-presentations nil)]
        (is (= [create-presentation-result] result))
        (is (spy/called-once-with? sql/query nil [get-all-presentations-query 20 0])))))

  (testing "Test get all presentations with limit specified"
    (with-redefs [sql/query (spy/stub [create-presentation-result])]
      (let [limit  1
            result (sut/get-all-presentations nil :limit limit)]
        (is (= [create-presentation-result] result))
        (is (spy/called-once-with? sql/query nil [get-all-presentations-query limit 0])))))

  (testing "Test get all presentations with offset specified"
    (with-redefs [sql/query (spy/stub [create-presentation-result])]
      (let [offset 20
            result (sut/get-all-presentations nil :offset offset)]
        (is (= [create-presentation-result] result))
        (is (spy/called-once-with? sql/query nil [get-all-presentations-query 20 offset])))))

  (testing "Test get all presentation filter URLs from non-public"
    (with-redefs [sql/query (spy/stub [create-presentation-result create-presentation-result-not-public])]
      (let [result (sut/get-all-presentations nil)]
        (is (= [create-presentation-result (dissoc create-presentation-result-not-public :presentations/url)] result))
        (is (spy/called-once-with? sql/query nil [get-all-presentations-query 20 0]))))))

(def ^:private delete-presentation-result
  "Stub data for delete presentation call."
  {:next.jdbc/update-count 1})

(def ^:private delete-presentation-id
  "Test API presentation-id."
  33)

(deftest delete-presentation-test
  (testing "Test delete presentation with valid `presentation-id`"
    (with-redefs [sql/delete! (spy/stub delete-presentation-result)]
      (let [result (sut/delete-presentation nil delete-presentation-id)]
        (is (= (:next.jdbc/update-count delete-presentation-result) result))
        (is (spy/called-once-with? sql/delete! nil :presentations {:id delete-presentation-id}))))))
