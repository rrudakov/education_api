(ns education.http.endpoints.upload-test
  (:require
   [clojure.java.io :as io]
   [clojure.test :refer [deftest is testing]]
   [education.config :as config]
   [education.http.constants :as const]
   [education.http.endpoints.test-app :as test-app]
   [education.http.endpoints.upload :as sut]
   [education.test-data :as td]
   [peridot.multipart :as mp]
   [ring.mock.request :as mock]
   [spy.core :as spy]))

(def ^:private test-img-name
  "Image name to be uploaded.

  Must be real image name from `test-resources` folder."
  "1.png")

(deftest uuid-test
  (testing "Test `uuid` function returns unique string every time"
    (let [first-uuid  (sut/uuid)
          second-uuid (sut/uuid)]
      (is (not= first-uuid second-uuid)))))

(deftest file->byte-array-test
  (testing "Test file can be converted to byte-array"
    (let [f   (io/file (io/resource test-img-name))
          res (sut/file->byte-array f)]
      (is (instance? (class (byte-array 1)) res))
      (is (pos-int? (count res))))))

(def ^:private test-uuid
  "Just prefix to be added to filename."
  "prefix")

(deftest upload-test
  (testing "Test POST /upload successfully"
    (with-redefs [sut/write-file (spy/spy)
                  sut/uuid       (spy/stub test-uuid)]
      (let [file       (io/file (io/resource test-img-name))
            app        (test-app/api-routes-with-auth)
            response   (app (-> (mock/request :post "/api/upload")
                                (merge (mp/build {:file file}))))
            body       (test-app/parse-body (:body response))
            [[f name]] (spy/calls sut/write-file)]
        (is (= 200 (:status response)))
        (is (= {:url (str (config/base-url td/test-config) "/img/" test-uuid ".png")} body))
        (is (= (str (config/storage-path td/test-config) "img/" test-uuid ".png") name))
        (is (= (slurp f) (slurp file))))))

  (testing "Test POST /upload with invalid request"
    (with-redefs [sut/write-file (spy/spy)]
      (let [app      (test-app/api-routes-with-auth)
            response (-> (mock/request :post "/api/upload")
                         (merge (mp/build {:wrong (io/file (io/resource test-img-name))}))
                         (app))
            body     (test-app/parse-body (:body response))]
        (is (= 400 (:status response)))
        (is (= {:message const/bad-request-error-message
                :errors  ["Field file is mandatory"]}
               body))))))
