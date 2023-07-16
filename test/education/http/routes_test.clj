(ns education.http.routes-test
  (:require
   [cljc.java-time.instant :as instant]
   [clojure.spec.alpha :as s]
   [clojure.test :refer [deftest is testing]]
   [education.http.constants :as const]
   [education.http.endpoints.dresses :as dresses-endpoint]
   [education.http.endpoints.test-app :as test-app]
   [education.specs.common :as spec]
   [ring.mock.request :as mock]
   [ring.util.http-response :as status]
   [spy.core :as spy]))

(def ^:private dress-response-invalid
  "Invalid API dress response."
  {:id          22
   :title       "Title"
   :description "Description"
   :size        38
   :pictures    ["invalid url"]
   :price       "22.7"
   :created_on  (instant/now)
   :updated_on  (instant/now)})

(deftest response-validation-handler-test
  (testing "Test exception handler for invalid response body"
    (with-redefs [dresses-endpoint/get-dress-by-id-handler (spy/stub (status/ok dress-response-invalid))]
      (let [app      (test-app/api-routes-with-auth)
            response (app (mock/request :get "/api/dresses/22"))
            body     (test-app/parse-body (:body response))]
        (is (= 500 (:status response)))
        (is (= {:message const/server-error-message
                :details (s/explain-str ::spec/dress-response dress-response-invalid)}
               body))))))
