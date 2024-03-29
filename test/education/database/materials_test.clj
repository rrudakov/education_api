(ns education.database.materials-test
  (:require
   [cljc.java-time.instant :as instant]
   [clojure.test :refer [deftest is testing]]
   [education.database.materials :as sut]
   [next.jdbc.sql :as sql]
   [spy.core :as spy]))

(def ^:private instant-stub
  (instant/now))

(def ^:private create-material-request
  "Test create material API request."
  {:title       "New material"
   :description "Long material description"
   :preview     "https://alenkinaskazka.nl/img/preview.png"
   :store_link  "https://some.store.com/external/link"
   :price       "2.99"})

(def ^:private create-material-result
  "Stub data for create material call."
  {:materials/id          22
   :materials/title       "New material"
   :materials/description "Long material description"
   :materials/preview     "https://alenkinaskazka.nl/img/preview.png"
   :materials/store_link  "https://some.store.com/external/link"
   :materials/price       (bigdec "2.99")
   :materials/created_on  (instant/now)
   :materials/updated_on  (instant/now)})

(def ^:private create-material-body-expected
  "Expected body passed to SQL call."
  (update create-material-request :price bigdec))

(deftest add-material-test
  (testing "Test all material with correct fields"
    (with-redefs [sql/insert! (spy/stub create-material-result)]
      (let [result (sut/add-material nil create-material-request)]
        (is (= (:materials/id create-material-result) result))
        (is (spy/called-once-with? sql/insert! nil :materials create-material-body-expected))))))

(def ^:private update-material-request
  "Test update material-request."
  create-material-request)

(def ^:private update-material-id
  "Test API material-id."
  88)

(def ^:private update-material-body-expected
  "Expected body passed to SQL call."
  (-> update-material-request
      (update :price bigdec)
      (assoc :updated_on instant-stub)))

(deftest update-material-test
  (testing "Test update material with all fields"
    (with-redefs [sql/update! (spy/stub {:next.jdbc/update-count 1})
                  instant/now (spy/stub instant-stub)]
      (let [result (sut/update-material nil update-material-id update-material-request)]
        (is (= 1 result))
        (is (spy/called-once-with? sql/update! nil :materials update-material-body-expected {:id update-material-id}))))))

(def ^:private get-material-id
  "Test API material-id."
  99)

(deftest get-material-by-id-test
  (testing "Test get material by ID with valid `material-id`"
    (with-redefs [sql/get-by-id (spy/stub create-material-result)]
      (let [result (sut/get-material-by-id nil get-material-id)]
        (is (= create-material-result result))
        (is (spy/called-once-with? sql/get-by-id nil :materials get-material-id))))))

(deftest get-all-materials-test
  (testing "Test get all materials without optional parameters"
    (with-redefs [sql/find-by-keys (spy/stub [create-material-result])]
      (let [result (sut/get-all-materials nil)]
        (is (= [create-material-result] result))
        (is (spy/called-once-with? sql/find-by-keys nil :materials :all {:limit 20 :offset 0})))))

  (testing "Test get all materials with `limit` specified"
    (with-redefs [sql/find-by-keys (spy/stub [create-material-result])]
      (let [limit  (inc (rand-int 100))
            result (sut/get-all-materials nil :limit limit)]
        (is (= [create-material-result] result))
        (is (spy/called-once-with? sql/find-by-keys nil :materials :all {:limit limit :offset 0})))))

  (testing "Test get all materials with `offset` specified"
    (with-redefs [sql/find-by-keys (spy/stub [create-material-result])]
      (let [offset (inc (rand-int 100))
            result (sut/get-all-materials nil :offset offset)]
        (is (= [create-material-result] result))
        (is (spy/called-once-with? sql/find-by-keys nil :materials :all {:limit 20 :offset offset}))))))

(def ^:private delete-material-result
  "Stub data for delete material call."
  {:next.jdbc/update-count 1})

(def ^:private delete-material-id
  "Test API material-id."
  (inc (rand-int 100)))

(deftest delete-material-test
  (testing "Test delete material with valid `material-id`"
    (with-redefs [sql/delete! (spy/stub delete-material-result)]
      (let [result (sut/delete-material nil delete-material-id)]
        (is (= (:next.jdbc/update-count delete-material-result) result))
        (is (spy/called-once-with? sql/delete! nil :materials {:id delete-material-id}))))))
