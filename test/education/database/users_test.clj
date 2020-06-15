(ns education.database.users-test
  (:require [buddy.hashers :as hs]
            [clojure.test :refer :all]
            [education.database.roles :as roles]
            [education.database.users :as sut]
            [next.jdbc.sql :as sql]
            [spy.core :as spy]))

(def password
  "Password for test users."
  "123456")

(def test-user1
  "First mocked testing user."
  {:users/id            4,
   :users/user_name     "rrudakov",
   :users/user_password (hs/encrypt password),
   :users/user_email    "rrudakof@pm.me",
   :users/created_on    #inst "2020-05-07T15:13:36.388217000-00:00",
   :users/updated_on    #inst "2020-05-07T15:13:36.388217000-00:00"})

(def test-user2
  "Second mocked testing user."
  {:users/id            11,
   :users/user_name     "rrudakov2",
   :users/user_password (hs/encrypt password),
   :users/user_email    "rrudakov@pm.me2",
   :users/created_on    #inst "2020-05-08T20:20:15.867832000-00:00",
   :users/updated_on    #inst "2020-05-09T19:33:55.358506000-00:00"})

(def user-roles1
  "Roles for first mocked user."
  #{:admin :guest})

(def user-roles2
  "Roles for second mocked user."
  #{:admin :guest :moderator})

(deftest get-user-successful-test
  (testing "Test get existing user successfully"
    (with-redefs [sql/get-by-id        (spy/stub test-user1)
                  roles/get-user-roles (spy/stub user-roles1)]
      (let [user-id 4
            result  (sut/get-user nil user-id)]
        (is (= (assoc test-user1 :users/roles user-roles1) result))
        (is (spy/called-once-with? sql/get-by-id nil :users user-id))))))

(deftest get-user-not-found-test
  (testing "Test get non-existing user"
    (with-redefs [sql/get-by-id        (spy/stub nil)
                  roles/get-user-roles (spy/stub user-roles1)]
      (is (= nil (sut/get-user nil 4)))
      (is (spy/not-called? roles/get-user-roles)))))

(deftest get-all-users-test
  (testing "Test fetching all users from database"
    (with-redefs [sql/query (spy/stub [(assoc test-user1 :roles user-roles1)
                                       (assoc test-user2 :roles user-roles2)])]
      (let [result (sut/get-all-users nil)]
      (is (= (list (assoc test-user1 :users/roles user-roles1)
                   (assoc test-user2 :users/roles user-roles2)) result))
      (is (spy/called-once-with? sql/query nil
                                 [(str "SELECT u.id, u.user_name, u.user_email, array_agg(r.role_name) AS roles, u.created_on, u.updated_on "
                                       "FROM users u "
                                       "LEFT JOIN user_roles ur ON ur.user_id = u.id "
                                       "LEFT JOIN roles r ON r.id = ur.role_id "
                                       "GROUP BY u.id "
                                       "ORDER BY u.id DESC")]))))))

(deftest auth-user-successful-test
  (with-redefs [sql/get-by-id        (spy/stub test-user1)
                roles/get-user-roles (spy/stub user-roles1)]
    (testing "Test successful authorization"
      (let [[res user] (sut/auth-user nil
                                      {:username (:users/user_name test-user1)
                                       :password password})]
        (is (= true res))
        (is (= (assoc test-user1 :users/roles user-roles1) (:user user)))))))

(deftest auth-user-not-found
  (with-redefs [sql/get-by-id (fn [_ _ _ _ _] nil)]
    (testing "Test authorization of non-existing user"
      (let [[res msg] (sut/auth-user nil {:username "nonexist"
                                          :password password})]
        (is (= false res))
        (is (= {:message "Invalid username or password"} msg))))))

(deftest auth-user-wrong-password
  (with-redefs [sql/get-by-id (fn [_ _ _ _ _] test-user1)]
    (testing "Test authorization with wrong password"
      (let [[res msg] (sut/auth-user nil {:username (:users/user_name test-user1)
                                          :password "wrong_password"})]
        (is (= false res))
        (is (= {:message "Invalid username or password"} msg))))))
