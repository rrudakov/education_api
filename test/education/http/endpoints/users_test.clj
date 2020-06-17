(ns education.http.endpoints.users-test
  (:require [clojure.test :refer [deftest testing is]]
            [education.database.users :as usersdb]
            [education.http.constants :refer :all]
            [education.http.endpoints.test-app :refer :all]
            [education.test-data :refer :all]
            [ring.mock.request :as mock]
            [spy.core :as spy]))

(def test-response-user1
  "Expected user response."
  {:id         (:users/id db-test-user1)
   :username   (:users/user_name db-test-user1)
   :email      (:users/user_email db-test-user1)
   :roles      (vec (map name user1-roles))
   :created_on (str (:users/created_on db-test-user1))
   :updated_on (str (:users/updated_on db-test-user1))})

(def test-response-user2
  "Expected user response."
  {:id         (:users/id db-test-user2)
   :username   (:users/user_name db-test-user2)
   :email      (:users/user_email db-test-user2)
   :roles      (vec (map name user2-roles))
   :created_on (str (:users/created_on db-test-user2))
   :updated_on (str (:users/updated_on db-test-user2))})

(deftest all-users-test
  (testing "Test GET /users authorized with moderator role"
    (with-redefs [usersdb/get-all-users (spy/stub [(assoc db-test-user2 :users/roles user2-roles)
                                                   (assoc db-test-user1 :users/roles user1-roles)])]
      (let [app      (test-api-routes-with-auth (spy/spy))
            response (app (-> (mock/request :get "/api/users")
                              (mock/header :authorization (test-auth-token #{:moderator}))))
            body     (parse-body (:body response))]
        (is (= 200 (:status response)))
        (is (spy/called-once? usersdb/get-all-users))
        (is (= [test-response-user2 test-response-user1] body)))))

  (testing "Test GET /users without authorization token"
    (with-redefs [usersdb/get-all-users (spy/spy)]
      (let [app      (test-api-routes-with-auth (spy/spy))
            response (app (mock/request :get "/api/users"))
            body     (parse-body (:body response))]
        (is (= 401 (:status response)))
        (is (= {:message not-authorized-error-message} body))
        (is (spy/not-called? usersdb/get-all-users)))))

  (testing "Test GET /users with guest role"
    (with-redefs [usersdb/get-all-users (spy/spy)]
      (let [app      (test-api-routes-with-auth (spy/spy))
            response (app (-> (mock/request :get "/api/users")
                              (mock/header :authorization (test-auth-token #{:guest}))))
            body     (parse-body (:body response))]
        (is (= 403 (:status response)))
        (is (= {:message no-access-error-message} body))
        (is (spy/not-called? usersdb/get-all-users))))))

(deftest get-user-test
  (testing "Test GET /users/:id authorized with moderator role"
    (with-redefs [usersdb/get-user (spy/stub (assoc db-test-user1 :users/roles user1-roles))]
      (let [user-id  (:users/id db-test-user1)
            app      (test-api-routes-with-auth (spy/spy))
            response (app (-> (mock/request :get (str "/api/users/" user-id))
                              (mock/header :authorization (test-auth-token #{:moderator}))))
            body     (parse-body (:body response))]
        (is (= 200 (:status response)))
        (is (= test-response-user1 body))
        (is (spy/called-once-with? usersdb/get-user nil user-id)))))

  (testing "Test GET /users/:id authorized with moderator role (no user in db)"
    (with-redefs [usersdb/get-user (spy/stub nil)]
      (let [user-id  43
            app      (test-api-routes-with-auth (spy/spy))
            response (app (-> (mock/request :get (str "/api/users/" user-id))
                              (mock/header :authorization (test-auth-token #{:moderator}))))
            body     (parse-body (:body response))]
        (is (= 404 (:status response)))
        (is (= {:message not-found-error-message} body))
        (is (spy/called-once-with? usersdb/get-user nil user-id)))))

  (testing "Test GET /users/:id without authorization token"
    (with-redefs [usersdb/get-user (spy/spy)]
      (let [app      (test-api-routes-with-auth (spy/spy))
            response (app (mock/request :get "/api/users/55"))
            body     (parse-body (:body response))]
        (is (= 401 (:status response)))
        (is (= {:message not-authorized-error-message} body))
        (is (spy/not-called? usersdb/get-user)))))

  (testing "Test GET /users/:id with guest role"
    (with-redefs [usersdb/get-user (spy/spy)]
      (let [app      (test-api-routes-with-auth (spy/spy))
            response (app (-> (mock/request :get "/api/users/53")
                              (mock/header :authorization (test-auth-token #{:guest}))))
            body     (parse-body (:body response))]
        (is (= 403 (:status response)))
        (is (= {:message no-access-error-message} body))
        (is (spy/not-called? usersdb/get-user)))))

  (testing "Test GET /users/:id with invalid id"
    (with-redefs [usersdb/get-user (spy/spy)]
      (let [app      (test-api-routes-with-auth (spy/spy))
            response (app (-> (mock/request :get "/api/users/invalid")
                              (mock/header :authorization (test-auth-token #{:admin}))))
            body     (parse-body (:body response))]
        (is (= 400 (:status response)))
        (is (= (sorted-set :details :message) (into (sorted-set) (keys body))))
        (is (= bad-request-error-message (:message body)))
        (is (spy/not-called? usersdb/get-user))))))
