(ns education.database.roles-test
  (:require [buddy.hashers :as hs]
            [clojure.test :refer :all]
            [education.database.roles :as sut]
            [education.test-data :refer :all]
            [next.jdbc.sql :as sql]
            [spy.core :as spy]))

(def db-user-roles
  "Result of database query for user roles."
  [{:roles/role_name "guest"}
   {:roles/role_name "admin"}])

(deftest get-all-roles-test
  (testing "Test get all roles from database"
    (with-redefs [sql/query (spy/stub db-all-roles)]
      (is (= (map (fn [role] (update-in role [:roles/role_name] keyword)) db-all-roles)
             (sut/get-all-roles nil)))
      (is (spy/called-once-with? sql/query nil ["SELECT * FROM roles"])))))

(deftest get-user-roles-successful-test
  (testing "Test fetch roles for existing user."
    (with-redefs [sql/query (spy/stub db-user-roles)]
      (is (= #{:admin :guest} (sut/get-user-roles nil db-test-user1)))
      (is (spy/called-once-with?
           sql/query
           nil
           ["SELECT r.role_name FROM user_roles ur LEFT JOIN roles r ON ur.role_id = r.id WHERE ur.user_id = ?"
            (:users/id db-test-user1)])))))
