(ns education.http.endpoints.users-test
  (:require [cheshire.core :as cheshire]
            [clojure.string :refer [blank?]]
            [clojure.test :refer [deftest is testing]]
            [education.database.users :as usersdb]
            [education.http.constants :as const]
            [education.http.endpoints.test-app
             :refer
             [parse-body api-routes-with-auth]]
            [education.test-data :as td]
            [ring.mock.request :as mock]
            [spy.core :as spy])
  (:import java.sql.SQLException))

(def test-response-user1
  "Expected user response."
  {:id         (:users/id td/db-test-user1)
   :username   (:users/user_name td/db-test-user1)
   :email      (:users/user_email td/db-test-user1)
   :roles      td/user1-roles
   :created_on (str (:users/created_on td/db-test-user1))
   :updated_on (str (:users/updated_on td/db-test-user1))})

(def test-response-user2
  "Expected user response."
  {:id         (:users/id td/db-test-user2)
   :username   (:users/user_name td/db-test-user2)
   :email      (:users/user_email td/db-test-user2)
   :roles      td/user2-roles
   :created_on (str (:users/created_on td/db-test-user2))
   :updated_on (str (:users/updated_on td/db-test-user2))})

(deftest all-users-test
  (testing "Test GET /users authorized with moderator role"
    (with-redefs [usersdb/get-all-users (spy/stub [(assoc td/db-test-user2 :users/roles td/user2-roles)
                                                   (assoc td/db-test-user1 :users/roles td/user1-roles)])]
      (let [app      (api-routes-with-auth (spy/spy))
            response (app (-> (mock/request :get "/api/users")
                              (mock/header :authorization (td/test-auth-token #{:moderator}))))
            body     (parse-body (:body response))]
        (is (= 200 (:status response)))
        (is (spy/called-once? usersdb/get-all-users))
        (is (= [test-response-user2 test-response-user1] body)))))

  (testing "Test GET /users without authorization token"
    (with-redefs [usersdb/get-all-users (spy/spy)]
      (let [app      (api-routes-with-auth (spy/spy))
            response (app (mock/request :get "/api/users"))
            body     (parse-body (:body response))]
        (is (= 401 (:status response)))
        (is (= {:message const/not-authorized-error-message} body))
        (is (spy/not-called? usersdb/get-all-users)))))

  (testing "Test GET /users with guest role"
    (with-redefs [usersdb/get-all-users (spy/spy)]
      (let [app      (api-routes-with-auth (spy/spy))
            response (app (-> (mock/request :get "/api/users")
                              (mock/header :authorization (td/test-auth-token #{:guest}))))
            body     (parse-body (:body response))]
        (is (= 403 (:status response)))
        (is (= {:message const/no-access-error-message} body))
        (is (spy/not-called? usersdb/get-all-users))))))

(deftest get-user-test
  (testing "Test GET /users/:id authorized with moderator role"
    (with-redefs [usersdb/get-user (spy/stub (assoc td/db-test-user1 :users/roles td/user1-roles))]
      (let [user-id  (:users/id td/db-test-user1)
            app      (api-routes-with-auth (spy/spy))
            response (app (-> (mock/request :get (str "/api/users/" user-id))
                              (mock/header :authorization (td/test-auth-token #{:moderator}))))
            body     (parse-body (:body response))]
        (is (= 200 (:status response)))
        (is (= test-response-user1 body))
        (is (spy/called-once-with? usersdb/get-user nil user-id)))))

  (testing "Test GET /users/:id authorized with moderator role (no user in db)"
    (with-redefs [usersdb/get-user (spy/stub nil)]
      (let [user-id  43
            app      (api-routes-with-auth (spy/spy))
            response (app (-> (mock/request :get (str "/api/users/" user-id))
                              (mock/header :authorization (td/test-auth-token #{:moderator}))))
            body     (parse-body (:body response))]
        (is (= 404 (:status response)))
        (is (= {:message const/not-found-error-message} body))
        (is (spy/called-once-with? usersdb/get-user nil user-id)))))

  (testing "Test GET /users/:id without authorization token"
    (with-redefs [usersdb/get-user (spy/spy)]
      (let [app      (api-routes-with-auth (spy/spy))
            response (app (mock/request :get "/api/users/55"))
            body     (parse-body (:body response))]
        (is (= 401 (:status response)))
        (is (= {:message const/not-authorized-error-message} body))
        (is (spy/not-called? usersdb/get-user)))))

  (testing "Test GET /users/:id with guest role"
    (with-redefs [usersdb/get-user (spy/spy)]
      (let [app      (api-routes-with-auth (spy/spy))
            response (app (-> (mock/request :get "/api/users/53")
                              (mock/header :authorization (td/test-auth-token #{:guest}))))
            body     (parse-body (:body response))]
        (is (= 403 (:status response)))
        (is (= {:message const/no-access-error-message} body))
        (is (spy/not-called? usersdb/get-user)))))

  (testing "Test GET /users/:id with invalid id"
    (with-redefs [usersdb/get-user (spy/spy)]
      (let [app      (api-routes-with-auth (spy/spy))
            response (app (-> (mock/request :get "/api/users/invalid")
                              (mock/header :authorization (td/test-auth-token #{:admin}))))
            body     (parse-body (:body response))]
        (is (= 400 (:status response)))
        (is (some #{:details :message} (keys body)))
        (is (= const/bad-request-error-message (:message body)))
        (is (spy/not-called? usersdb/get-user))))))

(deftest add-user-test
  (testing "Test POST /users with valid request body"
    (let [user-id 777]
      (with-redefs [usersdb/add-user (spy/stub user-id)]
        (let [app      (api-routes-with-auth (spy/spy))
              response (app (-> (mock/request :post "/api/users")
                                (mock/content-type "application/json")
                                (mock/body (cheshire/generate-string td/add-user1-request))))
              body     (:body response)]
          (is (= 201 (:status response)))
          (is (nil? body))
          (is (= (str user-id) (get (:headers response) "Location")))
          (is (spy/called-once-with? usersdb/add-user nil td/add-user1-request))))))

  (testing "Test POST /users conflict logins"
    (with-redefs [usersdb/add-user (spy/mock (fn [_ _] (throw (SQLException. "Conflict" "23505"))))]
      (let [app      (api-routes-with-auth (spy/spy))
            response (app (-> (mock/request :post "/api/users")
                              (mock/content-type "application/json")
                              (mock/body (cheshire/generate-string td/add-user1-request))))
            body     (parse-body (:body response))]
        (is (= 409 (:status response)))
        (is (= {:message const/conflict-error-message} body))
        (is (spy/called-once-with? usersdb/add-user nil td/add-user1-request)))))

  (testing "Test POST /users without required request fields"
    (with-redefs [usersdb/add-user (spy/spy)]
      (doseq [req-body (->> [:username :email :password]
                            (map #(cheshire/generate-string (dissoc td/add-user1-request %))))]
        (let [app      (api-routes-with-auth (spy/spy))
              response (app (-> (mock/request :post "/api/users")
                                (mock/content-type "application/json")
                                (mock/body req-body)))
              body     (parse-body (:body response))]
          (is (= 400 (:status response)))
          (is (some #{:message :details} (keys body)))
          (is (= const/bad-request-error-message (:message body)))
          (is (spy/not-called? usersdb/add-user)))))))

(deftest update-user-test
  (testing "Test PATCH /users/:id with valid request body and admin role"
    (let [user-id 6766]
      (with-redefs [usersdb/update-user (spy/stub 0)]
        (let [app      (api-routes-with-auth (spy/spy))
              response (app (-> (mock/request :patch (str "/api/users/" user-id))
                                (mock/content-type "application/json")
                                (mock/header :authorization (td/test-auth-token #{:admin}))
                                (mock/body (cheshire/generate-string td/update-user1-request))))
              body     (:body response)]
          (is (= 204 (:status response)))
          (is (blank? body))
          (is (spy/called-once-with? usersdb/update-user nil user-id td/update-user1-request))))))

  (testing "Test PATCH /users/:id with valid request body and moderator role"
    (with-redefs [usersdb/update-user (spy/spy)]
      (let [app      (api-routes-with-auth (spy/spy))
            response (app (-> (mock/request :patch "/api/users/342")
                              (mock/content-type "application/json")
                              (mock/header :authorization (td/test-auth-token #{:moderator}))
                              (mock/body (cheshire/generate-string td/update-user1-request))))
            body     (parse-body (:body response))]
        (is (= 403 (:status response)))
        (is (= {:message const/no-access-error-message} body))
        (is (spy/not-called? usersdb/update-user)))))

  (testing "Test PATCH /users/:id without authorization token"
    (with-redefs [usersdb/update-user (spy/spy)]
      (let [app      (api-routes-with-auth (spy/spy))
            response (app (-> (mock/request :patch "/api/users/342")
                              (mock/content-type "application/json")
                              (mock/body (cheshire/generate-string td/update-user1-request))))
            body     (parse-body (:body response))]
        (is (= 401 (:status response)))
        (is (= {:message const/not-authorized-error-message} body))
        (is (spy/not-called? usersdb/update-user)))))

  (testing "Test PATCH /users/:id with invalid user-id"
    (with-redefs [usersdb/update-user (spy/spy)]
      (let [app      (api-routes-with-auth (spy/spy))
            response (app (-> (mock/request :patch "/api/users/invalid")
                              (mock/content-type "application/json")
                              (mock/header :authorization (td/test-auth-token #{:admin}))
                              (mock/body (cheshire/generate-string td/update-user1-request))))
            body     (parse-body (:body response))]
        (is (= 400 (:status response)))
        (is (some #{:message :details} (keys body)))
        (is (= const/bad-request-error-message (:message body)))
        (is (spy/not-called? usersdb/update-user)))))

  (testing "Test PATCH /users/:id with invalid request body"
    (with-redefs [usersdb/update-user (spy/spy)]
      (doseq [req-body [{:roles ["god" "superhero"]}
                        {:roles []}]]
        (let [app      (api-routes-with-auth (spy/spy))
              response (app (-> (mock/request :patch "/api/users/invalid")
                                (mock/content-type "application/json")
                                (mock/header :authorization (td/test-auth-token #{:admin}))
                                (mock/body (cheshire/generate-string req-body))))
              body     (parse-body (:body response))]
          (is (= 400 (:status response)))
          (is (some #{:message :details} (keys body)))
          (is (= const/bad-request-error-message (:message body)))
          (is (spy/not-called? usersdb/update-user)))))))

(deftest delete-user-test
  (testing "Test DELETE /users/:id with valid user-id and admin role"
    (let [user-id 666]
      (with-redefs [usersdb/delete-user (spy/stub 0)]
        (let [app      (api-routes-with-auth (spy/spy))
              response (app (-> (mock/request :delete (str "/api/users/" user-id))
                                (mock/header :authorization (td/test-auth-token #{:admin}))))
              body     (:body response)]
          (is (= 204 (:status response)))
          (is (clojure.string/blank? body))
          (is (spy/called-once-with? usersdb/delete-user nil user-id))))))

  (testing "Test DELETE /users/:id with valid user-id and moderator role"
    (with-redefs [usersdb/delete-user (spy/spy)]
      (let [app      (api-routes-with-auth (spy/spy))
            response (app (-> (mock/request :delete "/api/users/1")
                              (mock/header :authorization (td/test-auth-token #{:moderator}))))
            body     (parse-body (:body response))]
        (is (= 403 (:status response)))
        (is (= {:message const/no-access-error-message} body))
        (is (spy/not-called? usersdb/delete-user)))))

  (testing "Test DELETE /users/:id without authorization token"
    (with-redefs [usersdb/delete-user (spy/spy)]
      (let [app      (api-routes-with-auth (spy/spy))
            response (app (-> (mock/request :delete "/api/users/43")))
            body     (parse-body (:body response))]
        (is (= 401 (:status response)))
        (is (= {:message const/not-authorized-error-message} body))
        (is (spy/not-called? usersdb/delete-user)))))

  (testing "Test DELETE /users/:id with invalid user-id"
    (with-redefs [usersdb/delete-user (spy/spy)]
      (let [app      (api-routes-with-auth (spy/spy))
            response (app (-> (mock/request :delete "/api/users/invalid")
                              (mock/header :authorization (td/test-auth-token #{:admin}))))
            body     (parse-body (:body response))]
        (is (= 400 (:status response)))
        (is (some #{:message :details} (keys body)))
        (is (= const/bad-request-error-message (:message body)))
        (is (spy/not-called? usersdb/delete-user))))))

(deftest login-test
  (testing "Test POST /login with valid credentials"
    (with-redefs [usersdb/auth-user (spy/stub td/db-user-auth-successful)]
      (let [app      (api-routes-with-auth (spy/spy))
            response (app (-> (mock/request :post "/api/login")
                              (mock/content-type "application/json")
                              (mock/body (cheshire/generate-string td/auth-user1-request))))
            body     (parse-body (:body response))]
        (is (= 200 (:status response)))
        (is (contains? body :token))
        (is (contains? (td/parse-token (:token body)) :user))
        (is (= (assoc td/auth-user-deserialized :roles (vec (map name td/user1-roles)))
               (->> body :token td/parse-token :user)))
        (is (spy/called-once-with? usersdb/auth-user nil td/auth-user1-request)))))

  (testing "Test POST /login bad credentials"
    (with-redefs [usersdb/auth-user (spy/stub [false {:message const/invalid-credentials-error-message}])]
      (let [app      (api-routes-with-auth (spy/spy))
            response (app (-> (mock/request :post "/api/login")
                              (mock/content-type "application/json")
                              (mock/body (cheshire/generate-string td/auth-user1-request))))
            body     (parse-body (:body response))]
        (is (= 401 (:status response)))
        (is (= {:message const/invalid-credentials-error-message} body))
        (is (spy/called-once-with? usersdb/auth-user nil td/auth-user1-request))))))
