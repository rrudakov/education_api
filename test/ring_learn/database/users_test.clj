(ns ring-learn.database.users-test
  (:require [clojure.test :as t]
            [next.jdbc.sql :as sql]
            [ring-learn.database.roles :as roles]
            [ring-learn.database.users :as sut]
            [buddy.hashers :as hs]))

(def password
  "Password for test users."
  "123456")

(def test-user1
  "First mocked testing user."
  {:users/id 4,
   :users/user_name "rrudakov",
   :users/user_password (hs/encrypt password),
   :users/user_email "rrudakof@pm.me",
   :users/created_on #inst "2020-05-07T15:13:36.388217000-00:00",
   :users/updated_on #inst "2020-05-07T15:13:36.388217000-00:00"})

(def test-user2
  "Second mocked testing user."
  {:users/id 11,
   :users/user_name "rrudakov2",
   :users/user_password (hs/encrypt password),
   :users/user_email "rrudakov@pm.me2",
   :users/created_on #inst "2020-05-08T20:20:15.867832000-00:00",
   :users/updated_on #inst "2020-05-09T19:33:55.358506000-00:00"})

(def user-roles1
  "Roles for first mocked user."
  #{:admin :guest})

(def user-roles2
  "Roles for second mocked user."
  #{:admin :guest :moderator})

(t/deftest get-user-successful-test
  (with-redefs-fn {#'sql/get-by-id (fn [_ _ _] test-user1)
                   #'roles/get-user-roles (fn [_ _] user-roles1)}
    #(t/testing "Test get existing user successfully"
       (t/is (= (assoc test-user1 :users/roles user-roles1)
                (sut/get-user nil 4))
             "Wrong user was returned"))))

(t/deftest get-user-not-found-test
  (with-redefs-fn {#'sql/get-by-id (fn [_ _ _] nil)
                   #'roles/get-user-roles (fn [_ _] user-roles1)}
    #(t/testing "Test get non-existing user"
       (t/is (= nil
                (sut/get-user nil 4))
             "Result should be nil"))))

(t/deftest get-all-users-test
  (with-redefs-fn {#'sql/query (fn [_ _] [test-user1 test-user2])
                   #'roles/get-user-roles (fn [_ user]
                                            (let [id (:users/id user)]
                                              (condp = id
                                                (:users/id test-user1) user-roles1
                                                (:users/id test-user2) user-roles2)))}
    #(t/testing "Test fetching all users from database"
       (t/is (= (list (assoc test-user1 :users/roles user-roles1)
                      (assoc test-user2 :users/roles user-roles2))
                (sut/get-all-users nil))
             "Wrong user list"))))

(t/deftest auth-user-successful-test
  (with-redefs-fn {#'sql/get-by-id (fn [_ _ _ _ _] test-user1)
                   #'roles/get-user-roles (fn [_ _] user-roles1)}
    #(t/testing "Test successful authorization"
       (let [[res user] (sut/auth-user nil {:username (:users/user_name test-user1)
                                            :password password})]
         (t/is (= true res) "Wrong authorization result")
         (t/is (= (assoc test-user1 :users/roles user-roles1) (:user user))
               "Wrong authenticated user object")))))

(t/deftest auth-user-not-found
  (with-redefs-fn {#'sql/get-by-id (fn [_ _ _ _ _] nil)}
    #(t/testing "Test authorization of non-existing user"
       (let [[res msg] (sut/auth-user nil {:username "nonexist"
                                           :password password})]
         (t/is (= false res) "Wrong authorization result")
         (t/is (= {:message "Invalid username or password"} msg)
               "Wrong error message")))))

(t/deftest auth-user-wrong-password
  (with-redefs-fn {#'sql/get-by-id (fn [_ _ _ _ _] test-user1)}
    #(t/testing "Test authorization with wrong password"
       (let [[res msg] (sut/auth-user nil {:username (:users/user_name test-user1)
                                           :password "wrong_password"})]
         (t/is (= false res) "Wrong authorization result")
         (t/is (= {:message "Invalid username or password"} msg)
               "Wrong error message")))))
