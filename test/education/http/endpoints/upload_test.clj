(ns education.http.endpoints.upload-test
  (:require [clojure.test :refer [deftest is testing]]
            [education.http.endpoints.test-app :as test-app]
            [education.http.endpoints.upload :as sut]
            [peridot.multipart :as mp]
            [ring.mock.request :as mock]
            [spy.core :as spy]
            [clojure.java.io :as io]
            [education.config :as config]
            [education.test-data :as td]
            [education.http.constants :as const]))

(def ^:private test-uuid
  "Just prefix to be added to filename."
  "prefix")

(def ^:private test-img-name
  "Image name to be uploaded.

Must be real image name from `test-resources` folder."
  "1.png")

(deftest upload-test
  (testing "Test POST /upload successfully"
    (with-redefs [sut/write-file (spy/spy)
                  sut/uuid       (spy/stub test-uuid)]
      (let [app        (test-app/api-routes-with-auth (spy/spy))
            response   (app (-> (mock/request :post "/api/upload")
                                (merge (mp/build {:file (io/file (io/resource test-img-name))}))))
            body       (test-app/parse-body (:body response))
            [[_ name]] (spy/calls sut/write-file)]
        (is (= 200 (:status response)))
        (is (= {:url (str (config/base-url td/test-config) "/img/" test-uuid "_" test-img-name)} body))
        (is (= (str (config/storage-path td/test-config) "img/" test-uuid "_" test-img-name) name )))))

  (testing "Test POST /upload with invalid request"
    (with-redefs [sut/write-file (spy/spy)]
      (let [app      (test-app/api-routes-with-auth (spy/spy))
            response (-> (mock/request :post "/api/upload")
                         (merge (mp/build {:wrong (io/file (io/resource test-img-name))}))
                         (app))
            body     (test-app/parse-body (:body response))]
        (is (= 400 (:status response)))
        (is (= {:message const/bad-request-error-message} (dissoc body :details)))))))
