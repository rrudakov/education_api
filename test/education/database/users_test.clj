(ns education.database.users-test
  (:require [buddy.hashers :as hs]
            [clojure.test :refer :all]
            [education.database.roles :as roles]
            [education.database.users :as sut]
            [education.http.constants :refer :all]
            [education.test-data :refer :all]
            [next.jdbc :as jdbc]
            [next.jdbc.protocols :as p]
            [next.jdbc.sql :as sql]
            [spy.core :as spy]))

(deftest add-user-test
  (testing "Test add user successful flow return user ID"
    (with-redefs [sql/insert!            (spy/stub db-test-user1)
                  roles/get-role-by-name (spy/stub db-guest-role)
                  p/-transact            (spy/mock (fn [_ f _] (f nil)))
                  jdbc/transact          (spy/mock (fn [_ f _] (do (f nil) (:users/id db-test-user1))))]
      (let [new-user-id                        (sut/add-user nil add-user1-request)
            [[_ users-table users-insert]
             [_ user-roles-table role-insert]] (spy/calls sql/insert!)]
        (is (= (:users/id db-test-user1) new-user-id))
        (is (spy/called-once-with? roles/get-role-by-name nil "guest"))
        (is (spy/called-n-times? sql/insert! 2))
        (is (= :users users-table))
        (is (= [:user_name :user_password :user_email] (keys users-insert)))
        (is (= {:user_name  (:username add-user1-request)
                :user_email (:email add-user1-request)}
               (select-keys users-insert [:user_name :user_email])))
        (is (hs/check (:password add-user1-request) (:user_password users-insert)))
        (is (= :user_roles user-roles-table))
        (is (= {:user_id (:users/id db-test-user1)
                :role_id (:roles/id db-guest-role)}
               role-insert))))))

(deftest get-user-test
  (testing "Test get existing user successfully"
    (with-redefs [sql/get-by-id        (spy/stub db-test-user1)
                  roles/get-user-roles (spy/stub user1-roles)]
      (let [user-id 4
            result  (sut/get-user nil user-id)]
        (is (= (assoc db-test-user1 :users/roles user1-roles) result))
        (is (spy/called-once-with? sql/get-by-id nil :users user-id))
        (is (spy/called-once-with? roles/get-user-roles nil db-test-user1)))))

  (testing "Test get non-existing user"
    (with-redefs [sql/get-by-id        (spy/stub nil)
                  roles/get-user-roles (spy/stub user1-roles)]
      (is (= nil (sut/get-user nil 4)))
      (is (spy/not-called? roles/get-user-roles)))))

(deftest get-all-users-test
  (testing "Test fetching all users from database"
    (with-redefs [sql/query (spy/stub [(assoc db-test-user1 :roles user1-roles)
                                       (assoc db-test-user2 :roles user2-roles)])]
      (let [result (sut/get-all-users nil)]
      (is (= (list (assoc db-test-user1 :users/roles user1-roles)
                   (assoc db-test-user2 :users/roles user2-roles)) result))
      (is (spy/called-once-with?
           sql/query nil
           [(str "SELECT u.id, u.user_name, u.user_email, array_agg(r.role_name) AS roles, u.created_on, u.updated_on "
                 "FROM users u "
                 "LEFT JOIN user_roles ur ON ur.user_id = u.id "
                 "LEFT JOIN roles r ON r.id = ur.role_id "
                 "GROUP BY u.id "
                 "ORDER BY u.id DESC")]))))))

(deftest update-user-test
  (testing "Test update user roles successful flow"
    (with-redefs [p/-transact         (spy/mock (fn [_ f _] (f nil)))
                  jdbc/transact       (spy/mock (fn [_ f _] (f nil)))
                  sql/delete!         (spy/stub 1)
                  sql/update!         (spy/stub 1)
                  sql/insert-multi!   (spy/stub 1)
                  roles/get-all-roles (spy/stub db-all-roles)]
      (let [user-id                  (:users/id db-test-user1)
            _                        (sut/update-user nil user-id update-user1-request)
            [[_ table query params]] (spy/calls sql/update!)]
        (is (spy/called-once? roles/get-all-roles))
        (is (spy/called-once-with? sql/delete! nil :user_roles {:user_id user-id}))
        (is (spy/called-once-with? sql/insert-multi!
                                   nil
                                   :user_roles
                                   [:user_id :role_id]
                                   [[user-id 1]
                                    [user-id 2]]))
        (is (= :users table))
        (is (= [:updated_on] (keys query)))
        (is (= {:id user-id} params))))))

(deftest delete-user-test
  (testing "Test delete user successful flow"
    (with-redefs [p/-transact   (spy/mock (fn [_ f _] (f nil)))
                  jdbc/transact (spy/mock (fn [_ f _] (f nil)))
                  sql/delete!   (spy/stub nil)]

      (let [user-id                              (:users/id db-test-user1)
            _                                    (sut/delete-user nil user-id)
            [[_ roles-table delete-roles-query]
             [_ users-table delete-users-query]] (spy/calls sql/delete!)]
        (is (= :user_roles roles-table))
        (is (= {:user_id user-id} delete-roles-query))
        (is (= :users users-table))
        (is (= {:id user-id} delete-users-query))))))

(deftest auth-user-test
  (testing "Test successful authorization"
    (with-redefs [sql/get-by-id        (spy/stub db-test-user1)
                  roles/get-user-roles (spy/stub user1-roles)]
      (let [[res user] (sut/auth-user nil auth-user1-request)]
        (is (= true res))
        (is (= (assoc db-test-user1 :users/roles user1-roles) (:user user)))
        (is (spy/called-once-with? sql/get-by-id nil :users (:username auth-user1-request) :user_name {}))
        (is (spy/called-once-with? roles/get-user-roles nil db-test-user1)))))

  (testing "Test authorization of non-existing user"
    (with-redefs [sql/get-by-id        (spy/stub nil)
                  roles/get-user-roles (spy/spy)]
      (let [[res msg] (sut/auth-user nil {:username "nonexist" :password password})]
        (is (= false res))
        (is (= {:message invalid-credentials-error-message} msg))
        (is (spy/not-called? roles/get-user-roles)))))

  (testing "Test authorization with wrong password"
    (with-redefs [sql/get-by-id        (spy/stub db-test-user1)
                  roles/get-user-roles (spy/spy)
                  hs/check             (spy/spy)]
      (let [wrong-password "wrong-password"
            [res msg]      (sut/auth-user nil {:username (:username auth-user1-request)
                                               :password wrong-password})]
        (is (= false res))
        (is (= {:message invalid-credentials-error-message} msg))
        (is (spy/not-called? roles/get-user-roles))
        (is (spy/called-once-with? hs/check wrong-password (:users/user_password db-test-user1)))))))
