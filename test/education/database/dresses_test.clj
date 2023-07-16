(ns education.database.dresses-test
  (:require
   [cljc.java-time.instant :as instant]
   [clojure.test :refer [deftest is testing]]
   [education.database.dresses :as sut]
   [next.jdbc.sql :as sql]
   [spy.core :as spy])
  (:import
   (java.time Instant)))

(def ^:private create-dress-request
  "Test create dress API request."
  {:title       "New dress"
   :description "Long dress description"
   :size        44
   :pictures    ["https://first.picture.net" "https://second.picture.net"]
   :price       "7.33"})

(def ^:private create-dress-result
  "Stub data for create dress call."
  {:dresses/id          22
   :dresses/title       "New dress"
   :dresses/description "Long dress description"
   :dresses/size        44
   :dresses/pictures    ["https://first.picture.net" "https://second.picture.net"]
   :dresses/price       (bigdec "7.33")
   :dresses/created_on  (instant/now)
   :dresses/updated_on  (instant/now)})

(def ^:private create-dress-body-expected
  "Expected body passed to SQL call."
  {:title       (:title create-dress-request)
   :description (:description create-dress-request)
   :size        (:size create-dress-request)
   :price       (bigdec (:price create-dress-request))})

(deftest add-dress-test
  (testing "Test add dress with correct fields"
    (with-redefs [sql/insert! (spy/stub create-dress-result)]
      (let [result                 (sut/add-dress nil create-dress-request)
            [[_ table query opts]] (spy/calls sql/insert!)]
        (is (= (:dresses/id create-dress-result) result))
        (is (= :dresses table))
        (is (nil? opts))
        (is (= create-dress-body-expected (dissoc query :pictures)))
        (is (contains? query :pictures))
        (is (instance? (class (into-array String [])) (:pictures query)))))))

(def ^:private update-dress-request
  "Test update dress API request."
  {:title       "Updated title"
   :description "Updated dress description"
   :size        22
   :pictures    ["https://first.updated.pic" "https://second.updated.pic"]
   :price       "88.99"})

(def ^:private update-dress-id
  "Test API dress-id."
  53)

(def ^:private update-dress-result
  "Stub data for update dress call."
  {:next.jdbc/update-count 1})

(def ^:private update-dress-body-expected
  "Expected body passed to SQL call."
  {:title       (:title update-dress-request)
   :description (:description update-dress-request)
   :size        (:size update-dress-request)
   :price       (bigdec (:price update-dress-request))})

(deftest update-dress-test
  (testing "Test update dress with all fields"
    (with-redefs [sql/update! (spy/stub update-dress-result)]
      (let [result                 (sut/update-dress nil update-dress-id update-dress-request)
            [[_ table query opts]] (spy/calls sql/update!)]
        (is (= (:next.jdbc/update-count update-dress-result) result))
        (is (= :dresses table))
        (is (= {:id update-dress-id} opts))
        (is (= update-dress-body-expected (dissoc query :pictures :updated_on)))
        (is (instance? (class (into-array String [])) (:pictures query)))
        (is (instance? Instant (:updated_on query)))))))

(def ^:private get-dress-id
  "Test API dress-id."
  828)

(deftest get-dress-by-id-test
  (testing "Test get dress by ID with valid `dress-id`"
    (with-redefs [sql/get-by-id (spy/stub create-dress-result)]
      (let [result (sut/get-dress-by-id nil get-dress-id)]
        (is (= create-dress-result result))
        (is (spy/called-once-with? sql/get-by-id nil :dresses get-dress-id))))))

(def ^:private get-all-dresses-query
  "Expected SQL query to get all dresses."
  "SELECT * FROM dresses ORDER BY updated_on DESC LIMIT ? OFFSET ?")

(deftest get-all-dresses-test
  (testing "Test get all dresses without optional parameters"
    (with-redefs [sql/query (spy/stub [create-dress-result])]
      (let [result (sut/get-all-dresses nil)]
        (is (= [create-dress-result] result))
        (is (spy/called-once-with? sql/query nil [get-all-dresses-query 20 0])))))

  (testing "Test get all dresses with limit specified"
    (with-redefs [sql/query (spy/stub [create-dress-result])]
      (let [limit  1
            result (sut/get-all-dresses nil :limit limit)]
        (is (= [create-dress-result] result))
        (is (spy/called-once-with? sql/query nil [get-all-dresses-query limit 0])))))

  (testing "Test get all dresses with offset specified"
    (with-redefs [sql/query (spy/stub [create-dress-result])]
      (let [offset 3
            result (sut/get-all-dresses nil :offset offset)]
        (is (= [create-dress-result] result))
        (is (spy/called-once-with? sql/query nil [get-all-dresses-query 20 offset]))))))

(def ^:private delete-dress-result
  "Stub data for delete dress call."
  {:next.jdbc/update-count 1})

(def ^:private delete-dress-id
  "Test API dress-id."
  989)

(deftest delete-dress-test
  (testing "Test delete dress with valid `dress-id`"
    (with-redefs [sql/delete! (spy/stub delete-dress-result)]
      (let [result (sut/delete-dress nil delete-dress-id)]
        (is (= (:next.jdbc/update-count delete-dress-result) result))
        (is (spy/called-once-with? sql/delete! nil :dresses {:id delete-dress-id}))))))
