(ns education.http.endpoints.dresses-test
  (:require [cheshire.core :as cheshire]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [education.database.dresses :as dresses-db]
            [education.http.constants :as const]
            [education.http.endpoints.test-app :as test-app]
            [education.test-data :as td]
            [ring.mock.request :as mock]
            [spy.core :as spy])
  (:import java.time.Instant))

(def ^:private test-dress-id
  "Test API `dress-id`."
  33)

(def ^:private create-dress-request
  "Test API request to create dress."
  {:title       "Dress title"
   :description "Dress description"
   :size        33
   :pictures    ["https://alenkinaskazka.net/img/177e97c8-db61-4490-a37b-7edb364ecc8c_Screenshot_20201008-203625.png"
                 "https://alenkinaskazka.net/img/6e8e5e12-8221-4a6b-9ac5-cf4fcaf49b6a_Screenshot_20201008-203702.png"
                 "https://alenkinaskazka.net/img/d4489b05-cb50-4d00-a6c6-bdbb93d0bc92_Screenshot_20201008-203826.png"]
   :price       "333.89"})

(deftest create-dress-test
  (doseq [request-body [create-dress-request
                        (assoc create-dress-request :pictures [])
                        (assoc create-dress-request :price "9999999")]]
    (testing "Test POST /dresses authorized with admin role and valid request body"
      (with-redefs [dresses-db/add-dress (spy/stub test-dress-id)]
        (let [role     :admin
              app      (test-app/api-routes-with-auth (spy/spy))
              response (app (-> (mock/request :post "/api/dresses")
                                (mock/content-type "application/json")
                                (mock/header :authorization (td/test-auth-token #{role}))
                                (mock/body (cheshire/generate-string request-body))))
              body     (test-app/parse-body (:body response))]
          (is (= 201 (:status response)))
          (is (= {:id test-dress-id} body))
          (is (spy/called-once-with? dresses-db/add-dress nil request-body))))))

  (doseq [role [:guest :moderator]]
    (testing (str "Test POST /dresses authorized with " role " role and valid request body")
      (with-redefs [dresses-db/add-dress (spy/spy)]
        (let [app      (test-app/api-routes-with-auth (spy/spy))
              response (app (-> (mock/request :post "/api/dresses")
                                (mock/content-type "application/json")
                                (mock/header :authorization (td/test-auth-token #{role}))
                                (mock/body (cheshire/generate-string create-dress-request))))
              body     (test-app/parse-body (:body response))]
          (is (= 403 (:status response)))
          (is (spy/not-called?  dresses-db/add-dress))
          (is (= {:message const/no-access-error-message} body))))))

  (testing "Test POST /dresses without authorization header"
    (with-redefs [dresses-db/add-dress (spy/spy)]
      (let [app      (test-app/api-routes-with-auth (spy/spy))
            response (app (-> (mock/request :post "/api/dresses")
                              (mock/content-type "application/json")
                              (mock/body (cheshire/generate-string create-dress-request))))
            body     (test-app/parse-body (:body response))]
        (is (= 401 (:status response)))
        (is (spy/not-called? dresses-db/add-dress))
        (is (= {:message const/not-authorized-error-message} body)))))

  (doseq [request-body [(dissoc create-dress-request :title)
                        (dissoc create-dress-request :description)
                        (dissoc create-dress-request :size)
                        (dissoc create-dress-request :pictures)
                        (dissoc create-dress-request :price)
                        (assoc create-dress-request :title (str/join (repeat 501 "a")))
                        (assoc create-dress-request :title 123)
                        (assoc create-dress-request :description 1234)
                        (assoc create-dress-request :size "33")
                        (assoc create-dress-request :size 32.2)
                        (assoc create-dress-request :pictures "https://some.nonlist.url")
                        (assoc create-dress-request :price "7 euro")
                        (assoc create-dress-request :price "-123.23")
                        (assoc create-dress-request :price 35.555)]]
    (testing "Test POST /dresses authorized with invalid request body"
      (with-redefs [dresses-db/add-dress (spy/spy)]
        (let [app      (test-app/api-routes-with-auth (spy/spy))
              response (app (-> (mock/request :post "/api/dresses")
                                (mock/content-type "application/json")
                                (mock/header :authorization (td/test-auth-token #{:admin}))
                                (mock/body (cheshire/generate-string request-body))))
              body     (test-app/parse-body (:body response))]
          (is (= 400 (:status response)))
          (is (spy/not-called? dresses-db/add-dress))
          (is (= {:message const/bad-request-error-message} (dissoc body :details))))))))

(def ^:private update-dress-request
  "Test API request to update dress."
  {:title       "Dress title"
   :description "Dress description"
   :size        33
   :pictures    ["https://alenkinaskazka.net/img/177e97c8-db61-4490-a37b-7edb364ecc8c_Screenshot_20201008-203625.png"
                 "https://alenkinaskazka.net/img/6e8e5e12-8221-4a6b-9ac5-cf4fcaf49b6a_Screenshot_20201008-203702.png"
                 "https://alenkinaskazka.net/img/d4489b05-cb50-4d00-a6c6-bdbb93d0bc92_Screenshot_20201008-203826.png"]
   :price       "333.89"})

(deftest update-dress-test
  (doseq [request-body [update-dress-request
                        (dissoc update-dress-request :title)
                        (dissoc update-dress-request :description)
                        (dissoc update-dress-request :size)
                        (dissoc update-dress-request :pictures)
                        (dissoc update-dress-request :price)
                        {}]]
    (testing "Test PATCH /dresses/:dress-id authorized with admin role and valid request body"
      (with-redefs [dresses-db/update-dress (spy/stub 1)]
        (let [role     :admin
              app      (test-app/api-routes-with-auth (spy/spy))
              response (app (-> (mock/request :patch (str "/api/dresses/" test-dress-id))
                                (mock/content-type "application/json")
                                (mock/header :authorization (td/test-auth-token #{role}))
                                (mock/body (cheshire/generate-string request-body))))]
          (is (= 204 (:status response)))
          (is (spy/called-once-with? dresses-db/update-dress nil test-dress-id request-body))
          (is (empty? (:body response)))))))

  (doseq [role [:guest :moderator]]
    (testing (str "Test PATCH /dresses/:dress-id authorized with " role " role and valid request body")
      (with-redefs [dresses-db/update-dress (spy/spy)]
        (let [app      (test-app/api-routes-with-auth (spy/spy))
              response (app (-> (mock/request :patch (str "/api/dresses/" test-dress-id))
                                (mock/content-type "appliation/json")
                                (mock/header :authorization (td/test-auth-token #{role}))
                                (mock/body (cheshire/generate-string update-dress-request))))
              body     (test-app/parse-body (:body response))]
          (is (= 403 (:status response)))
          (is (spy/not-called? dresses-db/update-dress))
          (is (= {:message const/no-access-error-message} body))))))

  (testing "Test PATCH /dresses/:dress-id without authorization token"
    (with-redefs [dresses-db/update-dress (spy/spy)]
      (let [app      (test-app/api-routes-with-auth (spy/spy))
            response (app (-> (mock/request :patch (str "/api/dresses/" test-dress-id))
                              (mock/content-type "application/json")
                              (mock/body (cheshire/generate-string update-dress-request))))
            body     (test-app/parse-body (:body response))]
        (is (= 401 (:status response)))
        (is (spy/not-called? dresses-db/update-dress))
        (is (= {:message const/not-authorized-error-message} body)))))

  (testing "Test PATCH /dresses/:dress-id with invalid `dress-id`"
    (with-redefs [dresses-db/update-dress (spy/spy)]
      (let [app      (test-app/api-routes-with-auth (spy/spy))
            response (app (-> (mock/request :patch "/api/dresses/invalid")
                              (mock/content-type "application/json")
                              (mock/header :authorization (td/test-auth-token #{:admin}))
                              (mock/body (cheshire/generate-string update-dress-request))))
            body     (test-app/parse-body (:body response))]
        (is (= 400 (:status response)))
        (is (spy/not-called? dresses-db/update-dress))
        (is (= {:message const/bad-request-error-message} (dissoc body :details))))))

  (testing "Test PATCH /dresses/:dress-id with non-existing `dress-id`"
    (with-redefs [dresses-db/update-dress (spy/stub 0)]
      (let [app      (test-app/api-routes-with-auth (spy/spy))
            response (app (-> (mock/request :patch (str "/api/dresses/" test-dress-id))
                              (mock/content-type "application/json")
                              (mock/header :authorization (td/test-auth-token #{:admin}))
                              (mock/body (cheshire/generate-string update-dress-request))))
            body     (test-app/parse-body (:body response))]
        (is (= 404 (:status response)))
        (is (spy/called-once-with? dresses-db/update-dress nil test-dress-id update-dress-request))
        (is (= {:message const/not-found-error-message} body)))))

  (testing "Test PATCH /dresses/:dress-id unexpected response from database"
    (with-redefs [dresses-db/update-dress (spy/stub 2)]
      (let [app      (test-app/api-routes-with-auth (spy/spy))
            response (app (-> (mock/request :patch (str "/api/dresses/" test-dress-id))
                              (mock/content-type "application/json")
                              (mock/header :authorization (td/test-auth-token #{:admin}))
                              (mock/body (cheshire/generate-string update-dress-request))))
            body     (test-app/parse-body (:body response))]
        (is (= 500 (:status response)))
        (is (spy/called-once-with? dresses-db/update-dress nil test-dress-id update-dress-request))
        (is (= {:message const/server-error-message} body)))))

  (doseq [request-body [(assoc update-dress-request :title (str/join (repeat 501 "a")))
                        (assoc update-dress-request :title 123)
                        (assoc update-dress-request :description 1234)
                        (assoc update-dress-request :size "33")
                        (assoc update-dress-request :size 32.2)
                        (assoc update-dress-request :pictures "https://some.nonlist.url")
                        (assoc update-dress-request :price "7 euro")
                        (assoc update-dress-request :price "-123.23")
                        (assoc update-dress-request :price 35.555)]]
    (testing "Test PATCH /dresses/:dress-id with invalid request body"
      (with-redefs [dresses-db/update-dress (spy/spy)]
        (let [app      (test-app/api-routes-with-auth (spy/spy))
              response (app (-> (mock/request :patch (str "/api/dresses/" test-dress-id))
                                (mock/content-type "application/json")
                                (mock/header :authorization (td/test-auth-token #{:admin}))
                                (mock/body (cheshire/generate-string request-body))))
              body     (test-app/parse-body (:body response))]
          (is (= 400 (:status response)))
          (is (spy/not-called? dresses-db/update-dress))
          (is (= {:message const/bad-request-error-message} (dissoc body :details))))))))

(def ^:private dress-from-db
  "Test dress database query result."
  {:dresses/id          888
   :dresses/title       "Dress title"
   :dresses/description "Dress description"
   :dresses/size        32
   :dresses/pictures    ["http://first.picture.com" "http://second.picture.com"]
   :dresses/price       (bigdec "8989.999999")
   :dresses/created_on  (Instant/parse "2020-12-12T18:22:12Z")
   :dresses/updated_on  (Instant/parse "2020-12-18T18:22:12Z")})

(def ^:private dress-response-expected
  "Expected API dress response."
  {:id          (:dresses/id dress-from-db)
   :title       (:dresses/title dress-from-db)
   :description (:dresses/description dress-from-db)
   :size        (:dresses/size dress-from-db)
   :pictures    (:dresses/pictures dress-from-db)
   :price       (format "%.2f" (:dresses/price dress-from-db))
   :created_on  (str (:dresses/created_on dress-from-db))
   :updated_on  (str (:dresses/updated_on dress-from-db))})

(deftest get-dress-by-id-test
  (testing "Test GET /dresses/:dress-id with valid `dress-id`"
    (with-redefs [dresses-db/get-dress-by-id (spy/stub dress-from-db)]
      (let [app      (test-app/api-routes-with-auth (spy/spy))
            response (app (mock/request :get (str "/api/dresses/" test-dress-id)))
            body     (test-app/parse-body (:body response))]
        (is (= 200 (:status response)))
        (is (= dress-response-expected body))
        (is (spy/called-once-with? dresses-db/get-dress-by-id nil test-dress-id))))))
