(ns education.http.restructure-test
  (:require [clojure.test :refer [deftest testing is]]
            [education.http.restructure :as sut]
            [education.test-data :refer [auth-user-deserialized]]))

(def users-expected-roles
  "Test data for `has-role?` test."
  [[(assoc auth-user-deserialized :roles #{:guest}) #{:guest} true]
   [(assoc auth-user-deserialized :roles #{:moderator}) #{:guest} true]
   [(assoc auth-user-deserialized :roles #{:admin}) #{:guest} true]
   [(assoc auth-user-deserialized :roles #{:guest}) #{:moderator} false]
   [(assoc auth-user-deserialized :roles #{:moderator}) #{:moderator} true]
   [(assoc auth-user-deserialized :roles #{:admin}) #{:moderator} true]
   [(assoc auth-user-deserialized :roles #{:guest}) #{:admin} false]
   [(assoc auth-user-deserialized :roles #{:moderator}) #{:admin} false]
   [(assoc auth-user-deserialized :roles #{:admin}) #{:admin} true]
   [(assoc auth-user-deserialized :roles #{:superhero}) #{:guest :admin :moderator} false]
   [(assoc auth-user-deserialized :roles #{:guest :moderator}) #{:admin} false]
   [(assoc auth-user-deserialized :roles #{:admin}) #{:guest :moderator :admin} true]])

(deftest has-role?-test
  (doseq [[user required-roles expected-result] users-expected-roles]
    (testing "has-role?"
      (is (= expected-result (sut/has-role? user required-roles))))))
