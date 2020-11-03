(ns education.database.lessons-test
  (:require [clojure.test :refer [deftest is testing]]
            [education.database.lessons :as sut]
            [next.jdbc.sql :as sql]
            [spy.core :as spy])
  (:import java.time.Instant))

(def ^:private create-lesson-request
  "Test create lesson API request."
  {:title           "Video lesson"
   :subtitle        "Video subtitle"
   :description     "Long video lesson description"
   :screenshots     ["http://first.screenshot.com" "http://second.screenshot.com"]
   :price           "22.50"})

(def ^:private create-lesson-result
  "Stub data for create article call."
  {:lessons/id              19
   :lessons/title           "Video lesson"
   :lessons/subtitle        "Video subtitle"
   :lessons/description     "Long video lesson description"
   :lessons/screenshots     ["http://first.screenshot.com" "http://second.screenshot.com"]
   :lessons/price           (bigdec "22.50000")
   :lessons/created_on      (Instant/now)
   :lessons/updated_on      (Instant/now)})

(def ^:private expected-create-body
  {:title           (:title create-lesson-request)
   :subtitle        (:subtitle create-lesson-request)
   :description     (:description create-lesson-request)
   :price           (bigdec (:price create-lesson-request))})

(deftest add-lesson-test
  (testing "Test add lesson with correct fields"
    (with-redefs [sql/insert! (spy/stub create-lesson-result)]
      (let [result                 (sut/add-lesson nil create-lesson-request)
            [[_ table query opts]] (spy/calls sql/insert!)]
        (is (= (:lessons/id create-lesson-result) result))
        (is (= :lessons table))
        (is (nil? opts))
        (is (= expected-create-body (dissoc query :screenshots)))
        (is (instance? (class (into-array String [])) (:screenshots query)))))))

(def ^:private update-lesson-request
  "Test update lesson API request."
  {:title       "Updated title"
   :description "Updated video description"
   :screenshots nil
   :price       "7.30"})

(def ^:private update-lesson-id
  "Test API lesson-id."
  19)

(def ^:private update-lesson-result
  "Stub data for update lesson call."
  {:next.jdbc/update-count 1})

(def ^:private expected-update-body
  {:title       (:title update-lesson-request)
   :description (:description update-lesson-request)
   :price       (bigdec (:price update-lesson-request))})

(deftest update-lesson-test
  (testing "Test update lesson with correct fields"
    (with-redefs [sql/update! (spy/stub update-lesson-result)]
      (let [result                 (sut/update-lesson nil update-lesson-id update-lesson-request)
            [[_ table query opts]] (spy/calls sql/update!)]
        (is (= (:next.jdbc/update-count update-lesson-result) result))
        (is (= :lessons table))
        (is (= {:id update-lesson-id} opts))
        (is (= expected-update-body (dissoc query :updated_on :screenshots)))
        (is (instance? (class (into-array String [])) (:screenshots query)))))))

(def ^:private get-lesson-id
  "Test API lesson-id."
  99)

(deftest get-lesson-by-id-test
  (testing "Test get lesson by ID with valid `lesson-id`"
    (with-redefs [sql/get-by-id (spy/stub create-lesson-result)]
      (let [result (sut/get-lesson-by-id nil get-lesson-id)]
        (is (= create-lesson-result result))
        (is (spy/called-once-with? sql/get-by-id nil :lessons get-lesson-id))))))

(def ^:private get-all-lessons-query
  "Expected SQL query for getting all lessons."
  "SELECT * FROM lessons ORDER BY created_on ASC LIMIT ? OFFSET ?")

(deftest get-all-lessons-test
  (testing "Test get all lessons without optional parameters"
    (with-redefs [sql/query (spy/stub [create-lesson-result])]
      (let [result (sut/get-all-lessons nil)]
        (is (= [create-lesson-result] result))
        (is (spy/called-once-with? sql/query nil [get-all-lessons-query 20 0])))))

  (testing "Test get all lessons with limit specified"
    (with-redefs [sql/query (spy/stub [create-lesson-result])]
      (let [limit  99
            result (sut/get-all-lessons nil :limit limit)]
        (is (= [create-lesson-result] result))
        (is (spy/called-once-with? sql/query nil [get-all-lessons-query limit 0])))))

  (testing "Test get all lessons with offset specified"
    (with-redefs [sql/query (spy/stub [create-lesson-result])]
      (let [offset 33
            result (sut/get-all-lessons nil :offset offset)]
        (is (= [create-lesson-result] result))
        (is (spy/called-once-with? sql/query nil [get-all-lessons-query 20 offset]))))))

(def ^:private delete-lesson-result
  "Stub data for delete lesson call."
  {:next.jdbc/update-count 1})

(def ^:private delete-lesson-id
  "Test API lesson-id."
  383)

(deftest delete-lesson-test
  (testing "Test delete lesson with valid `lesson-id`"
    (with-redefs [sql/delete! (spy/stub delete-lesson-result)]
      (let [result (sut/delete-lesson nil delete-lesson-id)]
        (is (= (:next.jdbc/update-count delete-lesson-result) result))
        (is (spy/called-once-with? sql/delete! nil :lessons {:id delete-lesson-id}))))))
