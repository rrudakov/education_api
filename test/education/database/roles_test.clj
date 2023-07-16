(ns education.database.roles-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [education.database.roles :as sut]
   [education.test-data :as td]
   [next.jdbc.sql :as sql]
   [spy.core :as spy]))

(def db-user-roles
  "Result of database query for user roles."
  [{:roles/role_name "guest"}
   {:roles/role_name "admin"}])

(deftest get-all-roles-test
  (testing "Test get all roles from database"
    (with-redefs [sql/query (spy/stub td/db-all-roles)]
      (is (= (map (fn [role] (update-in role [:roles/role_name] keyword)) td/db-all-roles)
             (sut/get-all-roles nil)))
      (is (spy/called-once-with? sql/query nil ["SELECT * FROM roles"])))))

(deftest get-user-roles-successful-test
  (testing "Test fetch roles for existing user."
    (with-redefs [sql/query (spy/stub db-user-roles)]
      (is (= #{:admin :guest} (sut/get-user-roles nil td/db-test-user1)))
      (is (spy/called-once-with?
           sql/query
           nil
           ["SELECT r.role_name FROM user_roles AS ur LEFT JOIN roles AS r ON ur.role_id = r.id WHERE ur.user_id = ?"
            (:users/id td/db-test-user1)])))))
