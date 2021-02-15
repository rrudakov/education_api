(ns education.http.routes-test
  (:require [clojure.test :refer [deftest is testing]]
            [education.http.constants :as const]
            [education.http.endpoints.presentations :as presentations-endpoint]
            [education.http.endpoints.test-app :as test-app]
            [ring.mock.request :as mock]
            [spy.core :as spy]
            [clojure.spec.alpha :as s]
            [education.specs.common :as spec])
  (:import java.time.Instant))

(def ^:private presentation-response-invalid
  "Invalid API presentation response."
  {:id          22
   :title       "Title"
   :url         "invalid url"
   :description "Some description"
   :created_on  (Instant/now)
   :updated_on  (Instant/now)})

(deftest response-validation-handler-test
  (testing "Test exception handler for invalid response body"
    (with-redefs [presentations-endpoint/get-presentation-by-id-handler (spy/stub presentation-response-invalid)]
      (let [app      (test-app/api-routes-with-auth)
            response (app (mock/request :get "/api/presentations/22"))
            body     (test-app/parse-body (:body response))]
        (is (= 500 (:status response)))
        (is (= {:message const/server-error-message
                :details (s/explain-str ::spec/presentation-response presentation-response-invalid)}
               body))))))
