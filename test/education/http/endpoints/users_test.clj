(ns education.http.endpoints.users-test
  (:require [cheshire.core :as cheshire]
            [clojure.test :refer [deftest is testing]]
            [education.database.users :as usersdb]
            [education.http.constants :refer :all]
            [education.http.endpoints.test-app :refer :all]
            [education.test-data :refer :all]
            [ring.mock.request :as mock]
            [spy.core :as spy])
  (:import java.sql.SQLException))

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
        (is (some #{:details :message} (keys body)))
        (is (= bad-request-error-message (:message body)))
        (is (spy/not-called? usersdb/get-user))))))

(deftest add-user-test
  (testing "Test POST /users with valid request body"
    (let [user-id 777]
      (with-redefs [usersdb/add-user (spy/stub user-id)]
        (let [app      (test-api-routes-with-auth (spy/spy))
              response (app (-> (mock/request :post "/api/users")
                                (mock/content-type "application/json")
                                (mock/body (cheshire/generate-string add-user1-request))))
              body     (:body response)]
         (is (= 201 (:status response)))
         (is (nil? body))
         (is (= (str user-id) (get (:headers response) "Location")))
         (is (spy/called-once-with? usersdb/add-user nil add-user1-request))))))

  (testing "Test POST /users conflict logins"
    (with-redefs [usersdb/add-user (spy/mock (fn [_ _] (throw (SQLException. "Conflict" "23505"))))]
      (let [app      (test-api-routes-with-auth (spy/spy))
            response (app (-> (mock/request :post "/api/users")
                              (mock/content-type "application/json")
                              (mock/body (cheshire/generate-string add-user1-request))))
            body     (parse-body (:body response))]
        (is (= 409 (:status response)))
        (is (= {:message conflict-error-message} body))
        (is (spy/called-once-with? usersdb/add-user nil add-user1-request)))))

  (testing "Test POST /users without required request fields"
    (with-redefs [usersdb/add-user (spy/spy)]
      (doseq [req-body (->> [:username :email :password]
                            (map #(cheshire/generate-string (dissoc add-user1-request %))))]
        (let [app      (test-api-routes-with-auth (spy/spy))
              response (app (-> (mock/request :post "/api/users")
                                (mock/content-type "application/json")
                                (mock/body req-body)))
              body     (parse-body (:body response))]
          (is (= 400 (:status response)))
          (is (some #{:message :details} (keys body)))
          (is (= bad-request-error-message (:message body)))
          (is (spy/not-called? usersdb/add-user)))))))

(deftest update-user-test
  (testing "Test PATCH /users/:id with valid request body and admin role"
    (let [user-id 6766]
      (with-redefs [usersdb/update-user (spy/stub 0)]
        (let [app      (test-api-routes-with-auth (spy/spy))
              response (app (-> (mock/request :patch (str "/api/users/" user-id))
                                (mock/content-type "application/json")
                                (mock/header :authorization (test-auth-token #{:admin}))
                                (mock/body (cheshire/generate-string update-user1-request))))
              body     (:body response)]
          (is (= 204 (:status response)))
          (is (clojure.string/blank? body))
          (is (spy/called-once-with? usersdb/update-user nil user-id update-user1-request))))))

  (testing "Test PATCH /users/:id with valid request body and moderator role"
    (with-redefs [usersdb/update-user (spy/spy)]
      (let [app      (test-api-routes-with-auth (spy/spy))
            response (app (-> (mock/request :patch "/api/users/342")
                              (mock/content-type "application/json")
                              (mock/header :authorization (test-auth-token #{:moderator}))
                              (mock/body (cheshire/generate-string update-user1-request))))
            body     (parse-body (:body response))]
        (is (= 403 (:status response)))
        (is (= {:message no-access-error-message} body))
        (is (spy/not-called? usersdb/update-user)))))

  (testing "Test PATCH /users/:id without authorization token"
    (with-redefs [usersdb/update-user (spy/spy)]
      (let [app      (test-api-routes-with-auth (spy/spy))
            response (app (-> (mock/request :patch "/api/users/342")
                              (mock/content-type "application/json")
                              (mock/body (cheshire/generate-string update-user1-request))))
            body     (parse-body (:body response))]
        (is (= 401 (:status response)))
        (is (= {:message not-authorized-error-message} body))
        (is (spy/not-called? usersdb/update-user)))))

  (testing "Test PATCH /users/:id with invalid user-id"
    (with-redefs [usersdb/update-user (spy/spy)]
      (let [app      (test-api-routes-with-auth (spy/spy))
            response (app (-> (mock/request :patch "/api/users/invalid")
                              (mock/content-type "application/json")
                              (mock/header :authorization (test-auth-token #{:admin}))
                              (mock/body (cheshire/generate-string update-user1-request))))
            body     (parse-body (:body response))]
        (is (= 400 (:status response)))
        (is (some #{:message :details} (keys body)))
        (is (= bad-request-error-message (:message body)))
        (is (spy/not-called? usersdb/update-user)))))

  (testing "Test PATCH /users/:id with invalid request body"
    (with-redefs [usersdb/update-user (spy/spy)]
      (doseq [req-body [{:roles ["god" "superhero"]}
                        {:roles []}]]
        (let [app      (test-api-routes-with-auth (spy/spy))
              response (app (-> (mock/request :patch "/api/users/invalid")
                                (mock/content-type "application/json")
                                (mock/header :authorization (test-auth-token #{:admin}))
                                (mock/body (cheshire/generate-string req-body))))
              body     (parse-body (:body response))]
         (is (= 400 (:status response)))
         (is (some #{:message :details} (keys body)))
         (is (= bad-request-error-message (:message body)))
         (is (spy/not-called? usersdb/update-user)))))))

(deftest delete-user-test
  (testing "Test DELETE /users/:id with valid user-id and admin role"
    (let [user-id 666]
      (with-redefs [usersdb/delete-user (spy/stub 0)]
        (let [app      (test-api-routes-with-auth (spy/spy))
              response (app (-> (mock/request :delete (str "/api/users/" user-id))
                                (mock/header :authorization (test-auth-token #{:admin}))))
              body     (:body response)]
          (is (= 204 (:status response)))
          (is (clojure.string/blank? body))
          (is (spy/called-once-with? usersdb/delete-user nil user-id))))))

  (testing "Test DELETE /users/:id with valid user-id and moderator role"
    (with-redefs [usersdb/delete-user (spy/spy)]
      (let [app      (test-api-routes-with-auth (spy/spy))
            response (app (-> (mock/request :delete "/api/users/1")
                              (mock/header :authorization (test-auth-token #{:moderator}))))
            body     (parse-body (:body response))]
        (is (= 403 (:status response)))
        (is (= {:message no-access-error-message} body))
        (is (spy/not-called? usersdb/delete-user)))))

  (testing "Test DELETE /users/:id without authorization token"
    (with-redefs [usersdb/delete-user (spy/spy)]
      (let [app      (test-api-routes-with-auth (spy/spy))
            response (app (-> (mock/request :delete "/api/users/43")))
            body     (parse-body (:body response))]
        (is (= 401 (:status response)))
        (is (= {:message not-authorized-error-message} body))
        (is (spy/not-called? usersdb/delete-user)))))

  (testing "Test DELETE /users/:id with invalid user-id"
    (with-redefs [usersdb/delete-user (spy/spy)]
      (let [app      (test-api-routes-with-auth (spy/spy))
            response (app (-> (mock/request :delete "/api/users/invalid")
                              (mock/header :authorization (test-auth-token #{:admin}))))
            body     (parse-body (:body response))]
        (is (= 400 (:status response)))
         (is (some #{:message :details} (keys body)))
         (is (= bad-request-error-message (:message body)))
         (is (spy/not-called? usersdb/delete-user))))))

(deftest login-test
  (testing "Test POST /login with valid credentials"
    (with-redefs [usersdb/auth-user (spy/stub db-user-auth-successful)]
      (let [app      (test-api-routes-with-auth (spy/spy))
            response (app (-> (mock/request :post "/api/login")
                              (mock/content-type "application/json")
                              (mock/body (cheshire/generate-string auth-user1-request))))
            body     (parse-body (:body response))]
        (is (= 200 (:status response)))
        (is (contains? body :token))
        (is (contains? (parse-token (:token body)) :user))
        (is (= (assoc auth-user-deserialized :roles (vec (map name user1-roles)))
               (->> body :token parse-token :user)))
        (is (spy/called-once-with? usersdb/auth-user nil auth-user1-request)))))

  (testing "Test POST /login bad credentials"
    (with-redefs [usersdb/auth-user (spy/stub [false {:message invalid-credentials-error-message}])]
      (let [app      (test-api-routes-with-auth (spy/spy))
            response (app (-> (mock/request :post "/api/login")
                              (mock/content-type "application/json")
                              (mock/body (cheshire/generate-string auth-user1-request))))
            body     (parse-body (:body response))]
        (is (= 401 (:status response)))
        (is (= {:message invalid-credentials-error-message} body))
        (is (spy/called-once-with? usersdb/auth-user nil auth-user1-request))))))
