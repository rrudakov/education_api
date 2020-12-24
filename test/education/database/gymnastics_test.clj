(ns education.database.gymnastics-test
  (:require [clojure.test :refer [deftest is testing]]
            [education.database.gymnastics :as sut]
            [next.jdbc.sql :as sql]
            [spy.core :as spy])
  (:import java.time.Instant))

(def ^:private created-on
  "Constant value of `created_on` field."
  (Instant/now))

(def ^:private updated-on
  "Constant value of `updated_on` field."
  (Instant/now))

(def ^:private create-gymnastic-request
  "Test create gymnastic API request."
  {:subtype_id  1
   :title       "New gymnastic"
   :description "New gymnastic description."
   :picture     "https://alenkinaskazka.net/img/picture.png"})

(def ^:private create-gymnastic-result
  "Stub data for create gymnastic call."
  {:gymnastics/id          32
   :gymnastics/subtype_id  1
   :gymnastics/title       "New gymnastic"
   :gymnastics/description "New gymnastic description."
   :gymnastics/picture     "https://alenkinaskazka.net/img/picture.png"
   :gymnastics/created_on  created-on
   :gymnastics/updated_on  updated-on})

(deftest add-gymnastic-test
  (testing "Test add gymnastic with correct fields"
    (with-redefs [sql/insert! (spy/stub create-gymnastic-result)]
      (let [result (sut/add-gymnastic nil create-gymnastic-request)]
        (is (= (:gymnastics/id create-gymnastic-result) result))
        (is (spy/called-once-with? sql/insert! nil :gymnastics create-gymnastic-request))))))

(def ^:private update-gymnastic-id
  "Test API gymnastic-id."
  88)

(def ^:private update-gymnastic-result
  "Stub data for update gymnastic call."
  {:next.jdbc/update-count 1})

(deftest update-gymnastic-test
  (doseq [request [{:subtype_id 1}
                   {:title "New title"}
                   {:description "New description"}
                   {}
                   {:picture nil}]]
    (testing "Test update gymnastic with valid fields"
      (with-redefs [sql/update! (spy/stub update-gymnastic-result)]
        (let [result (sut/update-gymnastic nil update-gymnastic-id request)]
          (is (= (:next.jdbc/update-count update-gymnastic-result) result))
          (is (spy/called-once-with? sql/update! nil :gymnastics request {:id update-gymnastic-id})))))))

(def ^:private get-gymnastic-id
  "Test API gymnastic-id."
  999)

(deftest get-gymnastic-by-id-test
  (testing "Test get gymnastic by ID with valid `gymnastic-id`"
    (with-redefs [sql/get-by-id (spy/stub create-gymnastic-result)]
      (let [result (sut/get-gymnastic-by-id nil get-gymnastic-id)]
        (is (= create-gymnastic-result result))
        (is (spy/called-once-with? sql/get-by-id nil :gymnastics get-gymnastic-id))))))

(def ^:private get-all-gymnastics-query
  "Expected SQL query to get all gymnastics."
  "SELECT * FROM gymnastics WHERE subtype_id = ? ORDER BY updated_on DESC LIMIT ? OFFSET ?")

(def ^:private get-all-gymnastics-subtype-id
  "Test API subtype-id."
  3)

(deftest get-all-gymnastics-test
  (testing "Test get all gymnastics without optional parameters"
    (with-redefs [sql/query (spy/stub [create-gymnastic-result])]
      (let [result (sut/get-all-gymnastics nil get-all-gymnastics-subtype-id)]
        (is (= [create-gymnastic-result] result))
        (is (spy/called-once-with?
             sql/query
             nil
             [get-all-gymnastics-query get-all-gymnastics-subtype-id 20 0])))))

  (testing "Test get all gymnastics with limit specified"
    (with-redefs [sql/query (spy/stub [])]
      (let [limit  1
            result (sut/get-all-gymnastics nil get-all-gymnastics-subtype-id :limit limit)]
        (is (= [] result))
        (is (spy/called-once-with?
             sql/query
             nil
             [get-all-gymnastics-query get-all-gymnastics-subtype-id limit 0])))))

  (testing "Test get all gymnastics with offset specified"
    (with-redefs [sql/query (spy/stub [])]
      (let [offset 20
            result (sut/get-all-gymnastics nil get-all-gymnastics-subtype-id :offset offset)]
        (is (= [] result))
        (is (spy/called-once-with?
             sql/query
             nil
             [get-all-gymnastics-query get-all-gymnastics-subtype-id 20 offset]))))))

(def ^:private delete-gymnastic-result
  "Stub data for delete gymnastic call."
  {:next.jdbc/update-count 1})

(def ^:private delete-gymnastic-id
  "Test API gymnastic id."
  123)

(deftest delete-gymnastic-test
  (testing "Test delete gymnastic with valid `gymnastic-id`"
    (with-redefs [sql/delete! (spy/stub delete-gymnastic-result)]
      (let [result (sut/delete-gymnastic nil delete-gymnastic-id)]
        (is (= (:next.jdbc/update-count delete-gymnastic-result) result))
        (is (spy/called-once-with? sql/delete! nil :gymnastics {:id delete-gymnastic-id}))))))
