(ns ring-learn.database.roles-test
  (:require [buddy.hashers :as hs]
            [clojure.test :as t]
            [next.jdbc.sql :as sql]
            [ring-learn.database.roles :as sut]))

(def db-roles
  "Mocked roles from database."
  [{:roles/id 1, :roles/role_name "admin"}
   {:roles/id 2, :roles/role_name "moderator"}
   {:roles/id 3, :roles/role_name "guest"}])

(def password
  "Password for mocked user."
  "123456")

(def test-user
  "First mocked testing user."
  {:users/id 4,
   :users/user_name "rrudakov",
   :users/user_password (hs/encrypt password),
   :users/user_email "rrudakof@pm.me",
   :users/created_on #inst "2020-05-07T15:13:36.388217000-00:00",
   :users/updated_on #inst "2020-05-07T15:13:36.388217000-00:00"})

(def db-user-roles
  "Mocked roles for `test-user`."
  [{:roles/role_name "guest"}
   {:roles/role_name "admin"}])

(t/deftest get-all-roles-test
  (with-redefs-fn {#'sql/query (fn [_ _] db-roles)}
    #(t/testing "Test get all roles from database"
       (t/is (= (map (fn [role]
                       (update-in role [:roles/role_name] keyword))
                     db-roles)
                (sut/get-all-roles nil))
             "Wrong roles returned from database."))))

(t/deftest get-user-roles-successful-test
  (with-redefs-fn {#'sql/query (fn [_ _] db-user-roles)}
    #(t/testing "Test fetch roles for existing user."
       (t/is (= #{:admin :guest}
                (sut/get-user-roles nil test-user))
             "Wrong roles returned for user."))))
