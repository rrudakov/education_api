(ns education.http.endpoints.presentations-test
  (:require [cheshire.core :as cheshire]
            [clojure.test :refer [deftest is testing]]
            [education.database.presentations :as presentations-db]
            [education.http.endpoints.test-app :as test-app]
            [education.test-data :as td]
            [ring.mock.request :as mock]
            [spy.core :as spy]
            [education.http.constants :as const]))

(def ^:private test-presentation-id
  "Test API `presentation-id`."
  83)

(def ^:private create-presentation-request
  "Test API request to create new presentation."
  {:title       "New presentation"
   :url         "https://google.com/api/presentation"
   :description "This presentation is about bla-bla-bla..."})

(deftest create-presentation-test
  (testing "Test POST /presentations authorized with admin role and valid request body"
    (with-redefs [presentations-db/add-presentation (spy/stub test-presentation-id)]
      (let [role     :admin
            app      (test-app/api-routes-with-auth)
            response (app (-> (mock/request :post "/api/presentations")
                              (mock/content-type td/application-json)
                              (mock/header :authorization (td/test-auth-token #{role}))
                              (mock/body (cheshire/generate-string create-presentation-request))))
            body     (test-app/parse-body (:body response))]
        (is (= 201 (:status response)))
        (is (= {:id test-presentation-id} body))
        (is (spy/called-once-with? presentations-db/add-presentation nil create-presentation-request)))))

  (doseq [role [:guest :moderator]]
    (testing (str "Test POST /presentations authorized with " role " role and valid request body")
      (with-redefs [presentations-db/add-presentation (spy/spy)]
        (let [app      (test-app/api-routes-with-auth)
              response (app (-> (mock/request :post "/api/presentations")
                                (mock/content-type td/application-json)
                                (mock/header :authorization (td/test-auth-token #{role}))
                                (mock/body (cheshire/generate-string create-presentation-request))))
              body     (test-app/parse-body (:body response))]
          (is (= 403 (:status response)))
          (is (spy/not-called? presentations-db/add-presentation))
          (is (= {:message const/no-access-error-message} body)))))))
