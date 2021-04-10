(ns education.http.endpoints.dresses-test
  (:require [clojure.string :as str]
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
              app      (test-app/api-routes-with-auth)
              response (app (-> (mock/request :post "/api/dresses")
                                (mock/header :authorization (td/test-auth-token #{role}))
                                (mock/json-body request-body)))
              body     (test-app/parse-body (:body response))]
          (is (= 201 (:status response)))
          (is (= {:id test-dress-id} body))
          (is (spy/called-once-with? dresses-db/add-dress nil request-body))))))

  (doseq [role [:guest :moderator]]
    (testing (str "Test POST /dresses authorized with " role " role and valid request body")
      (with-redefs [dresses-db/add-dress (spy/spy)]
        (let [app      (test-app/api-routes-with-auth)
              response (app (-> (mock/request :post "/api/dresses")
                                (mock/header :authorization (td/test-auth-token #{role}))
                                (mock/json-body create-dress-request)))
              body     (test-app/parse-body (:body response))]
          (is (= 403 (:status response)))
          (is (spy/not-called? dresses-db/add-dress))
          (is (= {:message const/no-access-error-message} body))))))

  (testing "Test POST /dresses without authorization header"
    (with-redefs [dresses-db/add-dress (spy/spy)]
      (let [app      (test-app/api-routes-with-auth)
            response (app (-> (mock/request :post "/api/dresses")
                              (mock/json-body create-dress-request)))
            body     (test-app/parse-body (:body response))]
        (is (= 401 (:status response)))
        (is (spy/not-called? dresses-db/add-dress))
        (is (= {:message const/not-authorized-error-message} body)))))

  (doseq [[request-body expected-errors] [[(dissoc create-dress-request :title)
                                           ["Field title is mandatory"]]
                                          [(dissoc create-dress-request :description)
                                           ["Field description is mandatory"]]
                                          [(dissoc create-dress-request :size)
                                           ["Field size is mandatory"]]
                                          [(dissoc create-dress-request :pictures)
                                           ["Field pictures is mandatory"]]
                                          [(dissoc create-dress-request :price)
                                           ["Field price is mandatory"]]
                                          [(assoc create-dress-request :title (str/join (repeat 501 "a")))
                                           ["Title must not be longer than 500 characters"]]
                                          [(assoc create-dress-request :title 123)
                                           ["Title is not valid"]]
                                          [(assoc create-dress-request :description 1234)
                                           ["Description is not valid"]]
                                          [(assoc create-dress-request :size "33")
                                           ["Size is not valid"]]
                                          [(assoc create-dress-request :size 32.2)
                                           ["Size is not valid"]]
                                          [(assoc create-dress-request :pictures "https://some.nonlist.url")
                                           ["Pictures is not valid"]]
                                          [(assoc create-dress-request :price "7 euro")
                                           ["Price is not valid"]]
                                          [(assoc create-dress-request :price "-123.23")
                                           ["Price is not valid"]]
                                          [(assoc create-dress-request :price 35.555)
                                           ["Price is not valid"]]]]
    (testing "Test POST /dresses authorized with invalid request body"
      (with-redefs [dresses-db/add-dress (spy/spy)]
        (let [app      (test-app/api-routes-with-auth)
              response (app (-> (mock/request :post "/api/dresses")
                                (mock/header :authorization (td/test-auth-token #{:admin}))
                                (mock/json-body request-body)))
              body     (test-app/parse-body (:body response))]
          (is (= 400 (:status response)))
          (is (spy/not-called? dresses-db/add-dress))
          (is (= {:message const/bad-request-error-message
                  :errors  expected-errors} (dissoc body :details))))))))

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
              app      (test-app/api-routes-with-auth)
              response (app (-> (mock/request :patch (str "/api/dresses/" test-dress-id))
                                (mock/header :authorization (td/test-auth-token #{role}))
                                (mock/json-body request-body)))]
          (is (= 204 (:status response)))
          (is (spy/called-once-with? dresses-db/update-dress nil test-dress-id request-body))
          (is (empty? (:body response)))))))

  (doseq [role [:guest :moderator]]
    (testing (str "Test PATCH /dresses/:dress-id authorized with " role " role and valid request body")
      (with-redefs [dresses-db/update-dress (spy/spy)]
        (let [app      (test-app/api-routes-with-auth)
              response (app (-> (mock/request :patch (str "/api/dresses/" test-dress-id))
                                (mock/header :authorization (td/test-auth-token #{role}))
                                (mock/json-body update-dress-request)))
              body     (test-app/parse-body (:body response))]
          (is (= 403 (:status response)))
          (is (spy/not-called? dresses-db/update-dress))
          (is (= {:message const/no-access-error-message} body))))))

  (testing "Test PATCH /dresses/:dress-id without authorization token"
    (with-redefs [dresses-db/update-dress (spy/spy)]
      (let [app      (test-app/api-routes-with-auth)
            response (app (-> (mock/request :patch (str "/api/dresses/" test-dress-id))
                              (mock/json-body update-dress-request)))
            body     (test-app/parse-body (:body response))]
        (is (= 401 (:status response)))
        (is (spy/not-called? dresses-db/update-dress))
        (is (= {:message const/not-authorized-error-message} body)))))

  (testing "Test PATCH /dresses/:dress-id with invalid `dress-id`"
    (with-redefs [dresses-db/update-dress (spy/spy)]
      (let [app      (test-app/api-routes-with-auth)
            response (app (-> (mock/request :patch "/api/dresses/invalid")
                              (mock/header :authorization (td/test-auth-token #{:admin}))
                              (mock/json-body update-dress-request)))
            body     (test-app/parse-body (:body response))]
        (is (= 400 (:status response)))
        (is (spy/not-called? dresses-db/update-dress))
        (is (= {:message const/bad-request-error-message
                :errors  ["Field dress-id is mandatory"]}
               body)))))

  (testing "Test PATCH /dresses/:dress-id with non-existing `dress-id`"
    (with-redefs [dresses-db/update-dress (spy/stub 0)]
      (let [app      (test-app/api-routes-with-auth)
            response (app (-> (mock/request :patch (str "/api/dresses/" test-dress-id))
                              (mock/header :authorization (td/test-auth-token #{:admin}))
                              (mock/json-body update-dress-request)))
            body     (test-app/parse-body (:body response))]
        (is (= 404 (:status response)))
        (is (spy/called-once-with? dresses-db/update-dress nil test-dress-id update-dress-request))
        (is (= {:message const/not-found-error-message} body)))))

  (testing "Test PATCH /dresses/:dress-id unexpected response from database"
    (with-redefs [dresses-db/update-dress (spy/stub 2)]
      (let [app      (test-app/api-routes-with-auth)
            response (app (-> (mock/request :patch (str "/api/dresses/" test-dress-id))
                              (mock/header :authorization (td/test-auth-token #{:admin}))
                              (mock/json-body update-dress-request)))
            body     (test-app/parse-body (:body response))]
        (is (= 500 (:status response)))
        (is (spy/called-once-with? dresses-db/update-dress nil test-dress-id update-dress-request))
        (is (= {:message const/server-error-message} body)))))

  (doseq [[request-body errors] [[(assoc update-dress-request :title (str/join (repeat 501 "a")))
                                  ["Title must not be longer than 500 characters"]]
                                 [(assoc update-dress-request :title 123)
                                  ["Title is not valid"]]
                                 [(assoc update-dress-request :description 1234)
                                  ["Description is not valid"]]
                                 [(assoc update-dress-request :size "33")
                                  ["Size is not valid"]]
                                 [(assoc update-dress-request :size 32.2)
                                  ["Size is not valid"]]
                                 [(assoc update-dress-request :pictures "https://some.nonlist.url")
                                  ["Pictures is not valid"]]
                                 [(assoc update-dress-request :price "7 euro")
                                  ["Price is not valid"]]
                                 [(assoc update-dress-request :price "-123.23")
                                  ["Price is not valid"]]
                                 [(assoc update-dress-request :price 35.555)
                                  ["Price is not valid"]]]]
    (testing "Test PATCH /dresses/:dress-id with invalid request body"
      (with-redefs [dresses-db/update-dress (spy/spy)]
        (let [app      (test-app/api-routes-with-auth)
              response (app (-> (mock/request :patch (str "/api/dresses/" test-dress-id))
                                (mock/header :authorization (td/test-auth-token #{:admin}))
                                (mock/json-body request-body)))
              body     (test-app/parse-body (:body response))]
          (is (= 400 (:status response)))
          (is (spy/not-called? dresses-db/update-dress))
          (is (= {:message const/bad-request-error-message
                  :errors  errors}
                 body)))))))

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
      (let [app      (test-app/api-routes-with-auth)
            response (app (mock/request :get (str "/api/dresses/" test-dress-id)))
            body     (test-app/parse-body (:body response))]
        (is (= 200 (:status response)))
        (is (= dress-response-expected body))
        (is (spy/called-once-with? dresses-db/get-dress-by-id nil test-dress-id)))))

  (testing "Test GET /dresses/:dress-id with non-existing `dress-id`"
    (with-redefs [dresses-db/get-dress-by-id (spy/stub nil)]
      (let [app      (test-app/api-routes-with-auth)
            response (app (mock/request :get (str "/api/dresses/" test-dress-id)))
            body     (test-app/parse-body (:body response))]
        (is (= 404 (:status response)))
        (is (= {:message const/not-found-error-message} body))
        (is (spy/called-once-with? dresses-db/get-dress-by-id nil test-dress-id)))))

  (testing "Test get /dresses/:dress-id with invalid `dress-id`"
    (with-redefs [dresses-db/get-dress-by-id (spy/spy)]
      (let [app      (test-app/api-routes-with-auth)
            response (app (mock/request :get "/api/dresses/invalid"))
            body     (test-app/parse-body (:body response))]
        (is (= 400 (:status response)))
        (is (= {:message const/bad-request-error-message
                :errors  ["Value is not valid"]} body))
        (is (spy/not-called? dresses-db/get-dress-by-id))))))

(def ^:private dress-from-db-extra
  "One more test dress database query result."
  {:dresses/id          22
   :dresses/title       "Dress title 2"
   :dresses/description "Dress description 2"
   :dresses/size        88
   :dresses/pictures    ["http://extra.picture.com" "https://extra.more.picture"]
   :dresses/price       (bigdec "2222")
   :dresses/created_on  (Instant/parse "2020-03-13T12:22:33Z")
   :dresses/updated_on  (Instant/parse "2020-04-13T12:22:33Z")})

(def ^:private dress-response-expected-extra
  "One more expected API dress response."
  {:id          (:dresses/id dress-from-db-extra)
   :title       (:dresses/title dress-from-db-extra)
   :description (:dresses/description dress-from-db-extra)
   :size        (:dresses/size dress-from-db-extra)
   :pictures    (:dresses/pictures dress-from-db-extra)
   :price       (format "%.2f" (:dresses/price dress-from-db-extra))
   :created_on  (str (:dresses/created_on dress-from-db-extra))
   :updated_on  (str (:dresses/updated_on dress-from-db-extra))})

(deftest get-all-dresses-test
  (testing "Test GET /dresses/ without any query parameters"
    (with-redefs [dresses-db/get-all-dresses (spy/stub [dress-from-db dress-from-db-extra])]
      (let [app      (test-app/api-routes-with-auth)
            response (app (mock/request :get "/api/dresses"))
            body     (test-app/parse-body (:body response))]
        (is (= 200 (:status response)))
        (is (= [dress-response-expected dress-response-expected-extra] body))
        (is (spy/called-once-with? dresses-db/get-all-dresses nil :limit nil :offset nil)))))

  (testing "Test GET /dresses/ with optional `limit` and `offset` parameters"
    (with-redefs [dresses-db/get-all-dresses (spy/stub [dress-from-db dress-from-db-extra])]
      (let [app          (test-app/api-routes-with-auth)
            limit-param  99
            offset-param 20
            response     (app (mock/request :get (str "/api/dresses?limit=" limit-param "&offset=" offset-param)))
            body         (test-app/parse-body (:body response))]
        (is (= 200 (:status response)))
        (is (= [dress-response-expected dress-response-expected-extra] body))
        (is (spy/called-once-with? dresses-db/get-all-dresses nil :limit limit-param :offset offset-param)))))

  (doseq [url ["/api/dresses?limit=invalid"
               "/api/dresses?offset=invalid"]]
    (testing "Test GET /dresses/ with invalid query parameters"
      (with-redefs [dresses-db/get-all-dresses (spy/spy)]
        (let [app      (test-app/api-routes-with-auth)
              response (app (mock/request :get url))
              body     (test-app/parse-body (:body response))]
          (is (= 400 (:status response)))
          (is (= {:message const/bad-request-error-message
                  :errors  ["Value is not valid"]}
                 body))
          (is (spy/not-called? dresses-db/get-all-dresses)))))))

(deftest delete-dress-by-id-test
  (testing "Test DELETE /dresses/:dress-id authorized with :admin role and valid `dress-id`"
    (with-redefs [dresses-db/delete-dress (spy/stub 1)]
      (let [app      (test-app/api-routes-with-auth)
            response (app (-> (mock/request :delete (str "/api/dresses/" test-dress-id))
                              (mock/header :authorization (td/test-auth-token #{:admin}))))]
        (is (= 204 (:status response)))
        (is (empty? (:body response)))
        (is (spy/called-once-with? dresses-db/delete-dress nil test-dress-id)))))

  (doseq [role [:moderator :guest]]
    (testing (str "Test DELETE /dresses/:dress-id authorized with " role " role and valid `dress-id`")
      (with-redefs [dresses-db/delete-dress (spy/spy)]
        (let [app      (test-app/api-routes-with-auth)
              response (app (-> (mock/request :delete (str "/api/dresses/" test-dress-id))
                                (mock/header :authorization (td/test-auth-token #{role}))))
              body     (test-app/parse-body (:body response))]
          (is (= 403 (:status response)))
          (is (= {:message const/no-access-error-message} body))
          (is (spy/not-called? dresses-db/delete-dress))))))

  (testing "Test DELETE /dresses/:dress-id without authorization header"
    (with-redefs [dresses-db/delete-dress (spy/spy)]
      (let [app      (test-app/api-routes-with-auth)
            response (app (mock/request :delete (str "/api/dresses/" test-dress-id)))
            body     (test-app/parse-body (:body response))]
        (is (= 401 (:status response)))
        (is (= {:message const/not-authorized-error-message} body))
        (is (spy/not-called? dresses-db/delete-dress)))))

  (testing "Test DELETE /dresses/:dress-id for non-existing `dress-id`"
    (with-redefs [dresses-db/delete-dress (spy/stub 0)]
      (let [app      (test-app/api-routes-with-auth)
            response (app (-> (mock/request :delete (str "/api/dresses/" test-dress-id))
                              (mock/header :authorization (td/test-auth-token #{:admin}))))
            body     (test-app/parse-body (:body response))]
        (is (= 404 (:status response)))
        (is (= {:message const/not-found-error-message} body))
        (is (spy/called-once-with? dresses-db/delete-dress nil test-dress-id)))))

  (testing "Test DELETE /dresses/:dress-id with invalid `dress-id`"
    (with-redefs [dresses-db/delete-dress (spy/spy)]
      (let [app      (test-app/api-routes-with-auth)
            response (app (-> (mock/request :delete "/api/dresses/invalid")
                              (mock/header :authorization (td/test-auth-token #{:admin}))))
            body     (test-app/parse-body (:body response))]
        (is (= 400 (:status response)))
        (is (= {:message const/bad-request-error-message
                :errors  ["Value is not valid"]}
               body))
        (is (spy/not-called? dresses-db/delete-dress)))))

  (testing "Test DELETE /dresses/:dress-id unexpected result from database"
    (with-redefs [dresses-db/delete-dress (spy/stub 2)]
      (let [app      (test-app/api-routes-with-auth)
            response (app (-> (mock/request :delete (str "/api/dresses/" test-dress-id))
                              (mock/header :authorization (td/test-auth-token #{:admin}))))
            body     (test-app/parse-body (:body response))]
        (is (= 500 (:status response)))
        (is (= {:message const/server-error-message} body))
        (is (spy/called-once-with? dresses-db/delete-dress nil test-dress-id))))))
