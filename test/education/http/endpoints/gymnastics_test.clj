(ns education.http.endpoints.gymnastics-test
  (:require [education.http.endpoints.gymnastics :as sut]
            [clojure.test :refer [testing deftest is]]
            [education.database.gymnastics :as gymnastics-db]
            [spy.core :as spy]
            [education.http.endpoints.test-app :as test-app]
            [ring.mock.request :as mock]
            [education.test-data :as td]
            [cheshire.core :as cheshire]
            [education.http.constants :as const]
            [clojure.string :as str])
  (:import java.time.Instant))

(def ^:private test-gymnastic-id
  "Test API `gymnastic-id`."
  838)

(def ^:private create-gymnastic-request
  "Test API request to create gymnastic."
  {:subtype_id  2
   :title       "Gymnastic title"
   :description "Gymnastic description"
   :picture     "https://alenkinaskazka.net/img/picture.png"})

(deftest create-gymnastic-test
  (doseq [request-body [create-gymnastic-request
                        (dissoc create-gymnastic-request :picture)
                        (assoc create-gymnastic-request :picture nil)]]
    (testing "Test POST /gymnastics authorized with admin role and valid request body"
      (with-redefs [gymnastics-db/add-gymnastic (spy/stub test-gymnastic-id)]
        (let [role     :admin
              app      (test-app/api-routes-with-auth (spy/spy))
              response (app (-> (mock/request :post "/api/gymnastics")
                                (mock/content-type "application/json")
                                (mock/header :authorization (td/test-auth-token #{role}))
                                (mock/body (cheshire/generate-string request-body))))
              body     (test-app/parse-body (:body response))]
          (is (= 201 (:status response)))
          (is (= {:id test-gymnastic-id} body))
          (is (spy/called-once-with? gymnastics-db/add-gymnastic nil request-body))))))

  (doseq [role [:guest :moderator]]
    (testing (str "Test POST /gymnastics authorized with " role " role and valid request body")
      (with-redefs [gymnastics-db/add-gymnastic (spy/spy)]
        (let [app      (test-app/api-routes-with-auth (spy/spy))
              response (app (-> (mock/request :post "/api/gymnastics")
                                (mock/content-type "application/json")
                                (mock/header :authorization (td/test-auth-token #{role}))
                                (mock/body (cheshire/generate-string create-gymnastic-request))))
              body     (test-app/parse-body (:body response))]
          (is (= 403 (:status response)))
          (is (spy/not-called? gymnastics-db/add-gymnastic))
          (is (= {:message const/no-access-error-message} body))))))

  (testing "Test POST /gymnastics without authorization token"
    (with-redefs [gymnastics-db/add-gymnastic (spy/spy)]
      (let [app      (test-app/api-routes-with-auth (spy/spy))
            response (app (-> (mock/request :post "/api/gymnastics")
                              (mock/content-type "application/json")
                              (mock/body (cheshire/generate-string create-gymnastic-request))))
            body     (test-app/parse-body (:body response))]
        (is (= 401 (:status response)))
        (is (spy/not-called? gymnastics-db/add-gymnastic))
        (is (= {:message const/not-authorized-error-message} body)))))

  (doseq [request-body [(dissoc create-gymnastic-request :subtype_id)
                        (dissoc create-gymnastic-request :title)
                        (dissoc create-gymnastic-request :description)
                        (assoc create-gymnastic-request :subtype_id "3")
                        (assoc create-gymnastic-request :subtype_id 0)
                        (assoc create-gymnastic-request :title (str/join (repeat 501 "a")))
                        (assoc create-gymnastic-request :title 123)
                        (assoc create-gymnastic-request :description 123)
                        (assoc create-gymnastic-request :picture 123)
                        (assoc create-gymnastic-request :picture "invalidUrl")]]
    (testing "Test POST /gymnastics authorized with invalid request body"
      (with-redefs [gymnastics-db/add-gymnastic (spy/spy)]
        (let [app      (test-app/api-routes-with-auth (spy/spy))
              response (app (-> (mock/request :post "/api/gymnastics")
                                (mock/content-type "application/json")
                                (mock/header :authorization (td/test-auth-token #{:admin}))
                                (mock/body (cheshire/generate-string request-body))))
              body     (test-app/parse-body (:body response))]
          (is (= 400 (:status response)))
          (is (spy/not-called? gymnastics-db/add-gymnastic))
          (is (= {:message const/bad-request-error-message} (dissoc body :details))))))))

(def ^:private update-gymnastic-request
  "Test API request to update gymnastic."
  create-gymnastic-request)

(deftest update-gymnastic-test
  (doseq [request-body [update-gymnastic-request
                        (dissoc update-gymnastic-request :title)
                        (dissoc update-gymnastic-request :subtype_id)
                        (dissoc update-gymnastic-request :picture)
                        (dissoc update-gymnastic-request :description)
                        (assoc update-gymnastic-request :picture nil)
                        {}]]
    (testing "Test PATCH /gymnastics/:gymnastic-id authorized with admin role and valid request body"
      (with-redefs [gymnastics-db/update-gymnastic (spy/stub 1)]
        (let [role     :admin
              app      (test-app/api-routes-with-auth (spy/spy))
              response (app (-> (mock/request :patch (str "/api/gymnastics/" test-gymnastic-id))
                                (mock/content-type td/application-json)
                                (mock/header :authorization (td/test-auth-token #{role}))
                                (mock/body (cheshire/generate-string request-body))))]
          (is (= 204 (:status response)))
          (is (spy/called-once-with? gymnastics-db/update-gymnastic nil test-gymnastic-id request-body))
          (is (empty? (:body response)))))))

  (doseq [role [:guest :moderator]]
    (testing (str "Test PATCH /gymnastics/:gymnastic-id authorized with " role " role and valie request body")
      (with-redefs [gymnastics-db/update-gymnastic (spy/spy)]
        (let [app      (test-app/api-routes-with-auth (spy/spy))
              response (app (-> (mock/request :patch (str "/api/gymnastics/" test-gymnastic-id))
                                (mock/content-type td/application-json)
                                (mock/header :authorization (td/test-auth-token #{role}))
                                (mock/body (cheshire/generate-string update-gymnastic-request))))
              body     (test-app/parse-body (:body response))]
          (is (= 403 (:status response)))
          (is (spy/not-called? gymnastics-db/update-gymnastic))
          (is (= {:message const/no-access-error-message} body))))))

  (testing "Test PATCH /gymnastics/gymnastic-id without authorization token"
    (with-redefs [gymnastics-db/update-gymnastic (spy/spy)]
      (let [app      (test-app/api-routes-with-auth (spy/spy))
            response (app (-> (mock/request :patch (str "/api/gymnastics/" test-gymnastic-id))
                              (mock/content-type td/application-json)
                              (mock/body (cheshire/generate-string update-gymnastic-request))))
            body     (test-app/parse-body (:body response))]
        (is (= 401 (:status response)))
        (is (spy/not-called? gymnastics-db/update-gymnastic))
        (is (= {:message const/not-authorized-error-message} body)))))

  (testing "Test PATCH /gymnastics/:gymnastic-id with invalid `gymnastic-id`"
    (with-redefs [gymnastics-db/update-gymnastic (spy/spy)]
      (let [app      (test-app/api-routes-with-auth (spy/spy))
            response (app (-> (mock/request :patch "/api/gymnastics/invalid")
                              (mock/content-type td/application-json)
                              (mock/header :authorization (td/test-auth-token #{:admin}))
                              (mock/body (cheshire/generate-string update-gymnastic-request))))
            body     (test-app/parse-body (:body response))]
        (is (= 400 (:status response)))
        (is (spy/not-called? gymnastics-db/update-gymnastic))
        (is (= {:message const/bad-request-error-message} (dissoc body :details))))))

  (testing "Test PATCH /gymnastics/:gymnastic-id with non-existing `gymnastic-id`"
    (with-redefs [gymnastics-db/update-gymnastic (spy/stub 0)]
      (let [app      (test-app/api-routes-with-auth (spy/spy))
            response (app (-> (mock/request :patch (str "/api/gymnastics/" test-gymnastic-id))
                              (mock/content-type td/application-json)
                              (mock/header :authorization (td/test-auth-token #{:admin}))
                              (mock/body (cheshire/generate-string update-gymnastic-request))))
            body     (test-app/parse-body (:body response))]
        (is (= 404 (:status response)))
        (is (spy/called-once-with? gymnastics-db/update-gymnastic nil test-gymnastic-id update-gymnastic-request))
        (is (= {:message const/not-found-error-message} body)))))

  (testing "Test PATCH /gymnastics/:gymnastic-id unexpected response from database"
    (with-redefs [gymnastics-db/update-gymnastic (spy/stub 2)]
      (let [app      (test-app/api-routes-with-auth (spy/spy))
            response (app (-> (mock/request :patch (str "/api/gymnastics/" test-gymnastic-id))
                              (mock/content-type td/application-json)
                              (mock/header :authorization (td/test-auth-token #{:admin}))
                              (mock/body (cheshire/generate-string update-gymnastic-request))))
            body     (test-app/parse-body (:body response))]
        (is (= 500 (:status response)))
        (is (spy/called-once-with? gymnastics-db/update-gymnastic nil test-gymnastic-id update-gymnastic-request))
        (is (= {:message const/server-error-message} body)))))

  (doseq [request-body [(assoc update-gymnastic-request :subtype_id 0)
                        (assoc update-gymnastic-request :subtype_id "32")
                        (assoc update-gymnastic-request :subtype_id nil)
                        (assoc update-gymnastic-request :title (str/join (repeat 501 "a")))
                        (assoc update-gymnastic-request :title 123)
                        (assoc update-gymnastic-request :title nil)
                        (assoc update-gymnastic-request :description 123)
                        (assoc update-gymnastic-request :description nil)
                        (assoc update-gymnastic-request :picture "invalid")
                        (assoc update-gymnastic-request :picture 123)]]
    (testing "Test PATCH /gymnastics/:gymnastic-id with invalid request body"
      (with-redefs [gymnastics-db/update-gymnastic (spy/spy)]
        (let [app      (test-app/api-routes-with-auth (spy/spy))
              response (app (-> (mock/request :patch (str "/api/gymnastics/" test-gymnastic-id))
                                (mock/content-type td/application-json)
                                (mock/header :authorization (td/test-auth-token #{:admin}))
                                (mock/body (cheshire/generate-string request-body))))
              body     (test-app/parse-body (:body response))]
          (is (= 400 (:status response)))
          (is (spy/not-called? gymnastics-db/update-gymnastic))
          (is (= {:message const/bad-request-error-message} (dissoc body :details))))))))

(def ^:private created-on
  "Constant `created_on` value for tests."
  (Instant/now))

(def ^:private updated-on
  "Constant `updated_on` value for tests."
  (Instant/now))

(def ^:private gymnastic-from-db
  "Test gymnastic database query result."
  {:gymnastics/id          22
   :gymnastics/subtype_id  2
   :gymnastics/title       "Gymnastic title"
   :gymnastics/description "Gymnastic description"
   :gymnastics/picture     "https://alenkinaskazka.net/img/picture.png"
   :gymnastics/created_on  created-on
   :gymnastics/updated_on  updated-on})

(def ^:private gymnastic-response-expected
  "Expected API gymnastic response."
  {:id          (:gymnastics/id gymnastic-from-db)
   :subtype_id  (:gymnastics/subtype_id gymnastic-from-db)
   :title       (:gymnastics/title gymnastic-from-db)
   :description (:gymnastics/description gymnastic-from-db)
   :picture     (:gymnastics/picture gymnastic-from-db)
   :created_on  (str (:gymnastics/created_on gymnastic-from-db))
   :updated_on  (str (:gymnastics/updated_on gymnastic-from-db))})

(deftest get-gymnastic-by-id-test
  (testing "Test GET /gymnastics/:gymnastic-id with valid `gymnastic-id`"
    (with-redefs [gymnastics-db/get-gymnastic-by-id (spy/stub gymnastic-from-db)]
      (let [app      (test-app/api-routes-with-auth (spy/spy))
            response (app (mock/request :get (str "/api/gymnastics/" test-gymnastic-id)))
            body     (test-app/parse-body (:body response))]
        (is (= 200 (:status response)))
        (is (spy/called-once-with? gymnastics-db/get-gymnastic-by-id nil test-gymnastic-id))
        (is (= gymnastic-response-expected body)))))

  (testing "Test GET /gymnastics/:gymnastic-id with non-existing `gymnastic-id`"
    (with-redefs [gymnastics-db/get-gymnastic-by-id (spy/stub nil)]
      (let [app      (test-app/api-routes-with-auth (spy/spy))
            response (app (mock/request :get (str "/api/gymnastics/" test-gymnastic-id)))
            body     (test-app/parse-body (:body response))]
        (is (= 404 (:status response)))
        (is (spy/called-once-with? gymnastics-db/get-gymnastic-by-id nil test-gymnastic-id))
        (is (= {:message const/not-found-error-message} body)))))

  (testing "Test GET /gymnastics/:gymnastic-id with invalid `gymnastic-id`"
    (with-redefs [gymnastics-db/get-gymnastic-by-id (spy/spy)]
      (let [app      (test-app/api-routes-with-auth (spy/spy))
            response (app (mock/request :get "/api/gymnastics/invalid"))
            body     (test-app/parse-body (:body response))]
        (is (= 400 (:status response)))
        (is (spy/not-called? gymnastics-db/get-gymnastic-by-id))
        (is (= {:message const/bad-request-error-message} (dissoc body :details)))))))

(def ^:private gymnastic-from-db-extra
  "One more test gymnastic database query result."
  {:gymnastics/id          23
   :gymnastics/subtype_id  2
   :gymnastics/title       "Gymnastic title 2"
   :gymnastics/description "Gymnastic description 2"
   :gymnastics/picture     "https://alenkinaskazka.net/img/picture2.png"
   :gymnastics/created_on  created-on
   :gymnastics/updated_on  updated-on})

(def ^:private gymnastic-response-expected-extra
  "one more expected API gymnastic response."
  {:id          (:gymnastics/id gymnastic-from-db-extra)
   :subtype_id  (:gymnastics/subtype_id gymnastic-from-db-extra)
   :title       (:gymnastics/title gymnastic-from-db-extra)
   :description (:gymnastics/description gymnastic-from-db-extra)
   :picture     (:gymnastics/picture gymnastic-from-db-extra)
   :created_on  (str (:gymnastics/created_on gymnastic-from-db-extra))
   :updated_on  (str (:gymnastics/updated_on gymnastic-from-db-extra))})

(deftest get-all-gymnastics-test
  (testing "Test GET /gymnastics/ with valid `subtype_id` parameter"
    (with-redefs [gymnastics-db/get-all-gymnastics (spy/stub [gymnastic-from-db gymnastic-from-db-extra])]
      (let [subtype-id 4
            app        (test-app/api-routes-with-auth (spy/spy))
            response   (app (mock/request :get (str "/api/gymnastics?subtype_id=" subtype-id)))
            body       (test-app/parse-body (:body response))]
        (is (= 200 (:status response)))
        (is (spy/called-once-with? gymnastics-db/get-all-gymnastics nil subtype-id :limit nil :offset nil))
        (is (= [gymnastic-response-expected gymnastic-response-expected-extra] body)))))

  (testing "Test GET /gymnastics/ with valid `subtype_id` and optional `limit` and `offset` parameters"
    (with-redefs [gymnastics-db/get-all-gymnastics (spy/stub [])]
      (let [subtype-id   3
            offset-param 20
            limit-param  20
            app          (test-app/api-routes-with-auth (spy/spy))
            response     (app (mock/request :get (str "/api/gymnastics?subtype_id=" subtype-id
                                                      "&limit=" limit-param
                                                      "&offset=" offset-param)))
            body         (test-app/parse-body (:body response))]
        (is (= 200 (:status response)))
        (is (spy/called-once-with? gymnastics-db/get-all-gymnastics
                                   nil
                                   subtype-id
                                   :limit limit-param
                                   :offset offset-param))
        (is (= [] body)))))

  (testing "Test GET /gymnastics/ without `subtype_id`"
    (with-redefs [gymnastics-db/get-all-gymnastics (spy/spy)]
      (let [app      (test-app/api-routes-with-auth (spy/spy))
            response (app (mock/request :get "/api/gymnastics"))
            body     (test-app/parse-body (:body response))]
        (is (= 400 (:status response)))
        (is (spy/not-called? gymnastics-db/get-all-gymnastics))
        (is (= {:message const/bad-request-error-message} (dissoc body :details))))))

  (testing "Test GET /gymnastics with invalid `subtype_id`"
    (with-redefs [gymnastics-db/get-all-gymnastics (spy/spy)]
      (let [app      (test-app/api-routes-with-auth (spy/spy))
            response (app (mock/request :get "/api/gymnastics?subtype_id=invalid"))
            body     (test-app/parse-body (:body response))]
        (is (= 400 (:status response)))
        (is (spy/not-called? gymnastics-db/get-all-gymnastics))
        (is (= {:message const/bad-request-error-message} (dissoc body :details))))))

  (doseq [url ["/api/gymnastics?subtype_id=3&limit=invalid"
               "/api/gymnastics?subtype_id=3&offset=invalid"]]
    (testing "Test GET /gymnastics with invalid `offset` and `limit` parameters"
      (with-redefs [gymnastics-db/get-all-gymnastics (spy/spy)]
        (let [app      (test-app/api-routes-with-auth (spy/spy))
              response (app (mock/request :get url))
              body     (test-app/parse-body (:body response))]
          (is (= 400 (:status response)))
          (is (spy/not-called? gymnastics-db/get-all-gymnastics))
          (is (= {:message const/bad-request-error-message} (dissoc body :details))))))))

(deftest delete-gymnastic-by-id-test
  (testing "Test DELETE /gymnastics/:gymnastic-id authorized with :admin role and valid `gymnastic-id`"
    (with-redefs [gymnastics-db/delete-gymnastic (spy/stub 1)]
      (let [app      (test-app/api-routes-with-auth (spy/spy))
            response (app (-> (mock/request :delete (str "/api/gymnastics/" test-gymnastic-id))
                              (mock/header :authorization (td/test-auth-token #{:admin}))))]
        (is (= 204 (:status response)))
        (is (spy/called-once-with? gymnastics-db/delete-gymnastic nil test-gymnastic-id))
        (is (empty? (:body response))))))

  (doseq [role [:moderator :guest]]
    (testing (str "Test DELETE /gymnastics/:gymnastic-id with " role " role and valid `gymnastic-id`")
      (with-redefs [gymnastics-db/delete-gymnastic (spy/spy)]
        (let [app      (test-app/api-routes-with-auth (spy/spy))
              response (app (-> (mock/request :delete (str "/api/gymnastics/" test-gymnastic-id))
                                (mock/header :authorization (td/test-auth-token #{role}))))
              body     (test-app/parse-body (:body response))]
          (is (= 403 (:status response)))
          (is (spy/not-called? gymnastics-db/delete-gymnastic))
          (is (= {:message const/no-access-error-message} body))))))

  (testing "Test DELETE /gymnastics/:gymnastic-id without authorization header"
    (with-redefs [gymnastics-db/delete-gymnastic (spy/spy)]
      (let [app      (test-app/api-routes-with-auth (spy/spy))
            response (app (mock/request :delete (str "/api/gymnastics/" test-gymnastic-id)))
            body     (test-app/parse-body (:body response))]
        (is (= 401 (:status response)))
        (is (spy/not-called? gymnastics-db/delete-gymnastic))
        (is (= {:message const/not-authorized-error-message} body)))))

  (testing "Test DELETE /gymnastics/:gymnastic-id with non-existing `gymnastic-id`"
    (with-redefs [gymnastics-db/delete-gymnastic (spy/stub 0)]
      (let [app      (test-app/api-routes-with-auth (spy/spy))
            response (app (-> (mock/request :delete (str "/api/gymnastics/" test-gymnastic-id))
                              (mock/header :authorization (td/test-auth-token #{:admin}))))
            body     (test-app/parse-body (:body response))]
        (is (= 404 (:status response)))
        (is (spy/called-once-with? gymnastics-db/delete-gymnastic nil test-gymnastic-id))
        (is (= {:message const/not-found-error-message} body)))))

  (testing "Test DELETE /gymnastics/:gymnastic-id with invalid `gymnastic-id`"
    (with-redefs [gymnastics-db/delete-gymnastic (spy/spy)]
      (let [app      (test-app/api-routes-with-auth (spy/spy))
            response (app (-> (mock/request :delete "/api/gymnastics/invalid")
                              (mock/header :authorization (td/test-auth-token #{:admin}))))
            body     (test-app/parse-body (:body response))]
        (is (= 400 (:status response)))
        (is (spy/not-called? gymnastics-db/delete-gymnastic))
        (is (= {:message const/bad-request-error-message} (dissoc body :details))))))

  (testing "Test DELETE /gymnastics/:gymnastic-id unexpected result from database"
    (with-redefs [gymnastics-db/delete-gymnastic (spy/stub 2)]
      (let [app      (test-app/api-routes-with-auth (spy/spy))
            response (app (-> (mock/request :delete (str "/api/gymnastics/" test-gymnastic-id))
                              (mock/header :authorization (td/test-auth-token #{:admin}))))
            body     (test-app/parse-body (:body response))]
        (is (= 500 (:status response)))
        (is (spy/called-once-with? gymnastics-db/delete-gymnastic nil test-gymnastic-id))
        (is (= {:message const/server-error-message} body))))))