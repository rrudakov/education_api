(ns education.http.endpoints.roles-test
  (:require [clojure.test :refer [deftest is testing]]
            [education.database.roles :as rolesdb]
            [education.http.constants :as const]
            [education.http.endpoints.test-app
             :refer
             [parse-body test-api-routes-with-auth]]
            [education.test-data :refer [db-all-roles test-auth-token]]
            [ring.mock.request :as mock]
            [spy.core :as spy]))

(deftest roles-test
  (testing "Test GET /roles with admin role"
    (with-redefs [rolesdb/get-all-roles (spy/stub db-all-roles)]
      (let [app      (test-api-routes-with-auth (spy/spy))
            response (app (-> (mock/request :get "/api/roles")
                              (mock/header :authorization (test-auth-token #{:admin}))))
            body     (parse-body (:body response))]
        (is (= 200 (:status response)))
        (is (= [{:id 3, :name "guest"}
                {:id 2, :name "moderator"}
                {:id 1, :name "admin"}] body))
        (is (spy/called-once-with? rolesdb/get-all-roles nil)))))

  (testing "Test GET /roles with moderator role"
    (with-redefs [rolesdb/get-all-roles (spy/spy)]
      (let [app      (test-api-routes-with-auth (spy/spy))
            response (app (-> (mock/request :get "/api/roles")
                              (mock/header :authorization (test-auth-token #{:moderator}))))
            body     (parse-body (:body response))]
        (is (= 403 (:status response)))
        (is (= {:message const/no-access-error-message} body))
        (is (spy/not-called? rolesdb/get-all-roles)))))

  (testing "Test GET /roles without authorization token"
    (with-redefs [rolesdb/get-all-roles (spy/spy)]
      (let [app      (test-api-routes-with-auth (spy/spy))
            response (app (-> (mock/request :get "/api/roles")))
            body     (parse-body (:body response))]
        (is (= 401 (:status response)))
        (is (= {:message const/not-authorized-error-message} body))
        (is (spy/not-called? rolesdb/get-all-roles))))))
