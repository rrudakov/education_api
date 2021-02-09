(ns education.http.endpoints.presentations-test
  (:require [cheshire.core :as cheshire]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [education.database.presentations :as presentations-db]
            [education.http.constants :as const]
            [education.http.endpoints.test-app :as test-app]
            [education.test-data :as td]
            [ring.mock.request :as mock]
            [spy.core :as spy])
  (:import java.time.Instant))

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
          (is (= {:message const/no-access-error-message} body))))))

  (testing "Test POST /presentations without authorization header"
    (with-redefs [presentations-db/add-presentation (spy/spy)]
      (let [app      (test-app/api-routes-with-auth)
            response (app (-> (mock/request :post "/api/presentations")
                              (mock/content-type td/application-json)
                              (mock/body (cheshire/generate-string create-presentation-request))))
            body     (test-app/parse-body (:body response))]
        (is (= 401 (:status response)))
        (is (spy/not-called? presentations-db/add-presentation))
        (is (= {:message const/not-authorized-error-message} body)))))

  (doseq [request-body [(dissoc create-presentation-request :title)
                        (dissoc create-presentation-request :url)
                        (dissoc create-presentation-request :description)
                        (assoc create-presentation-request :title (str/join (repeat 501 "a")))
                        (assoc create-presentation-request :title 123)
                        (assoc create-presentation-request :url "invalid")
                        (assoc create-presentation-request :url 2828)
                        (assoc create-presentation-request :description 1234)]]
    (testing "Test POST /presentations authorized with invalid request body"
      (let [app      (test-app/api-routes-with-auth)
            response (app (-> (mock/request :post "/api/presentations")
                              (mock/content-type td/application-json)
                              (mock/header :authorization (td/test-auth-token #{:admin}))
                              (mock/body (cheshire/generate-string request-body))))
            body     (test-app/parse-body (:body response))]
        (is (= 400 (:status response)))
        (is (spy/not-called? presentations-db/add-presentation))
        (is (= {:message const/bad-request-error-message} (dissoc body :details)))))))

(def ^:private update-presentation-request
  "Test API request to update presentation."
  {:title       "Presentation title"
   :url         "https://google.com/api/presentation/some/fancy/stuff/"
   :description "Updated description for presentation"})

(deftest update-presentation-test
  (doseq [request-body [update-presentation-request
                        (dissoc update-presentation-request :title)
                        (dissoc update-presentation-request :url)
                        (dissoc update-presentation-request :description)
                        {}]]
    (testing "Test PATCH /presentations/:presentation-id authorized with admin role and valid request body"
      (with-redefs [presentations-db/update-presentation (spy/stub 1)]
        (let [app      (test-app/api-routes-with-auth)
              response (app (-> (mock/request :patch (str "/api/presentations/" test-presentation-id))
                                (mock/content-type td/application-json)
                                (mock/header :authorization (td/test-auth-token #{:admin}))
                                (mock/body (cheshire/generate-string request-body))))]
          (is (= 204 (:status response)))
          (is (spy/called-once-with? presentations-db/update-presentation nil test-presentation-id request-body))
          (is (empty? (:body response)))))))

  (doseq [role [:guest :moderator]]
    (testing (str "Test PATCH /presentations/:presentation-id authorized with " role " role and valid request body")
      (with-redefs [presentations-db/update-presentation (spy/spy)]
        (let [app      (test-app/api-routes-with-auth)
              response (app (-> (mock/request :patch (str "/api/presentations/" test-presentation-id))
                                (mock/content-type td/application-json)
                                (mock/header :authorization (td/test-auth-token #{role}))
                                (mock/body (cheshire/generate-string update-presentation-request))))
              body     (test-app/parse-body (:body response))]
          (is (= 403 (:status response)))
          (is (spy/not-called? presentations-db/update-presentation))
          (is (= {:message const/no-access-error-message} body))))))

  (testing "Test PATCH /presentations/:presentation-id without authorization token"
    (with-redefs [presentations-db/update-presentation (spy/spy)]
      (let [app      (test-app/api-routes-with-auth)
            response (app (-> (mock/request :patch (str "/api/presentations/" test-presentation-id))
                              (mock/content-type td/application-json)
                              (mock/body (cheshire/generate-string update-presentation-request))))
            body     (test-app/parse-body (:body response))]
        (is (= 401 (:status response)))
        (is (spy/not-called? presentations-db/update-presentation))
        (is (= {:message const/not-authorized-error-message} body)))))

  (testing "Test PATCH /presentations/:presentation-id with invalid `presentation-id`"
    (with-redefs [presentations-db/update-presentation (spy/spy)]
      (let [app      (test-app/api-routes-with-auth)
            response (app (-> (mock/request :patch "/api/presentations/invalid")
                              (mock/content-type td/application-json)
                              (mock/header :authorization (td/test-auth-token #{:admin}))
                              (mock/body (cheshire/generate-string update-presentation-request))))
            body     (test-app/parse-body (:body response))]
        (is (= 400 (:status response)))
        (is (spy/not-called? presentations-db/update-presentation))
        (is (= {:message const/bad-request-error-message} (dissoc body :details))))))

  (testing "Test PATCH /presentations/:presentation-id with non-existing `presentation-id`"
    (with-redefs [presentations-db/update-presentation (spy/stub 0)]
      (let [app      (test-app/api-routes-with-auth)
            response (app (-> (mock/request :patch (str "/api/presentations/" test-presentation-id))
                              (mock/content-type td/application-json)
                              (mock/header :authorization (td/test-auth-token #{:admin}))
                              (mock/body (cheshire/generate-string update-presentation-request))))
            body     (test-app/parse-body (:body response))]
        (is (= 404 (:status response)))
        (is (spy/called-once-with? presentations-db/update-presentation nil test-presentation-id update-presentation-request))
        (is (= {:message const/not-found-error-message} body)))))

  (testing "Test PATCH /presentations/:presentation-id unexpected response from database"
    (with-redefs [presentations-db/update-presentation (spy/stub 2)]
      (let [app      (test-app/api-routes-with-auth)
            response (app (-> (mock/request :patch (str "/api/presentations/" test-presentation-id))
                              (mock/content-type td/application-json)
                              (mock/header :authorization (td/test-auth-token #{:admin}))
                              (mock/body (cheshire/generate-string update-presentation-request))))
            body     (test-app/parse-body (:body response))]
        (is (= 500 (:status response)))
        (is (spy/called-once-with? presentations-db/update-presentation nil test-presentation-id update-presentation-request))
        (is (= {:message const/server-error-message} body)))))

  (doseq [request-body [(assoc update-presentation-request :title (str/join (repeat 501 "a")))
                        (assoc update-presentation-request :title 123)
                        (assoc update-presentation-request :url "invalid")
                        (assoc update-presentation-request :url 123)
                        (assoc update-presentation-request :description 1234)]]
    (testing "Test PATCH /presentations/:presentation-id with invalid request body"
      (with-redefs [presentations-db/update-presentation (spy/spy)]
        (let [app      (test-app/api-routes-with-auth)
              response (app (-> (mock/request :patch (str "/api/presentations/" test-presentation-id))
                                (mock/content-type td/application-json)
                                (mock/header :authorization (td/test-auth-token #{:admin}))
                                (mock/body (cheshire/generate-string request-body))))
              body     (test-app/parse-body (:body response))]
          (is (= 400 (:status response)))
          (is (spy/not-called? presentations-db/update-presentation))
          (is (= {:message const/bad-request-error-message} (dissoc body :details))))))))

(def ^:private presentation-from-db
  "Test presentation database query result."
  {:presentations/id          23
   :presentations/title       "The presentation"
   :presentations/url         "https://google.com/presentations/api/bla"
   :presentations/description "This is presentation about bla-bla.."
   :presentations/created_on  (Instant/now)
   :presentations/updated_on  (Instant/now)})

(def ^:private presentation-response-expected
  "Expected API presentation response."
  {:id          (:presentations/id presentation-from-db)
   :title       (:presentations/title presentation-from-db)
   :url         (:presentations/url presentation-from-db)
   :description (:presentations/description presentation-from-db)
   :created_on  (str (:presentations/created_on presentation-from-db))
   :updated_on  (str (:presentations/updated_on presentation-from-db))})

(deftest get-presentation-by-id-test
  (testing "Test GET /presentations/:presentation-id with valid `presentation-id`"
    (with-redefs [presentations-db/get-presentation-by-id (spy/stub presentation-from-db)]
      (let [app      (test-app/api-routes-with-auth)
            response (app (mock/request :get (str "/api/presentations/" test-presentation-id)))
            body     (test-app/parse-body (:body response))]
        (is (= 200 (:status response)))
        (is (= presentation-response-expected body))
        (is (spy/called-once-with? presentations-db/get-presentation-by-id nil test-presentation-id)))))

  (testing "Test GET /presentations/:presentstion-id with non-existing `presentation-id`"
    (with-redefs [presentations-db/get-presentation-by-id (spy/spy)]
      (let [app      (test-app/api-routes-with-auth)
            response (app (mock/request :get (str "/api/presentations/" test-presentation-id)))
            body     (test-app/parse-body (:body response))]
        (is (= 404 (:status response)))
        (is (= {:message const/not-found-error-message} body))
        (is (spy/called-once-with? presentations-db/get-presentation-by-id nil test-presentation-id)))))

  (testing "Test get /presentations/:presentation-id with invalid `presentation-id`"
    (with-redefs [presentations-db/get-presentation-by-id (spy/spy)]
      (let [app      (test-app/api-routes-with-auth)
            response (app (mock/request :get "/api/presentations/invalid"))
            body     (test-app/parse-body (:body response))]
        (is (= 400 (:status response)))
        (is (spy/not-called? presentations-db/get-presentation-by-id))
        (is (= {:message const/bad-request-error-message} (dissoc body :details)))))))

(def ^:private presentation-from-db-extra
  "One more test presentation database query result."
  {:presentations/id          22
   :presentations/title       "The presentation 2"
   :presentations/url         "https://google.com/presentations/api2/bla"
   :presentations/description "This is another presentation about bla-bla.."
   :presentations/created_on  (Instant/now)
   :presentations/updated_on  (Instant/now)})

(def ^:private presentation-response-expected-extra
  "One more expected API presentation response."
  {:id          (:presentations/id presentation-from-db-extra)
   :title       (:presentations/title presentation-from-db-extra)
   :url         (:presentations/url presentation-from-db-extra)
   :description (:presentations/description presentation-from-db-extra)
   :created_on  (str (:presentations/created_on presentation-from-db-extra))
   :updated_on  (str (:presentations/updated_on presentation-from-db-extra))})

(deftest get-all-presentations-test
  (testing "Test GET /presentations without any query parameters"
    (with-redefs [presentations-db/get-all-presentations (spy/stub [presentation-from-db presentation-from-db-extra])]
      (let [app      (test-app/api-routes-with-auth)
            response (app (mock/request :get "/api/presentations"))
            body     (test-app/parse-body (:body response))]
        (is (= 200 (:status response)))
        (is (= [presentation-response-expected presentation-response-expected-extra] body))
        (is (spy/called-once-with? presentations-db/get-all-presentations nil :limit nil :offset nil)))))

  (testing "Test GET /presentations with optional `limit` and `offset` parameters"
    (with-redefs [presentations-db/get-all-presentations (spy/stub [presentation-from-db presentation-from-db-extra])]
      (let [app          (test-app/api-routes-with-auth)
            limit-param  838
            offset-param 99
            response     (app (mock/request :get (str "/api/presentations?limit=" limit-param "&offset=" offset-param)))
            body         (test-app/parse-body (:body response))]
        (is (= 200 (:status response)))
        (is (= [presentation-response-expected presentation-response-expected-extra] body))
        (is (spy/called-once-with? presentations-db/get-all-presentations nil :limit limit-param :offset offset-param)))))

  (doseq [url ["/api/presentations?limit=invalid"
               "/api/presentations?offset=invalid"]]
    (testing "Test GET /presentations with invalid query parameters"
      (with-redefs [presentations-db/get-all-presentations (spy/spy)]
        (let [app      (test-app/api-routes-with-auth)
              response (app (mock/request :get url))
              body     (test-app/parse-body (:body response))]
          (is (= 400 (:status response)))
          (is (= {:message const/bad-request-error-message} (dissoc body :details)))
          (is (spy/not-called? presentations-db/get-all-presentations)))))))

(deftest delete-presentation-by-id-test
  (testing "Test DELETE /presentations/:presentation-id authorized with :admin role and valid `presentation-id`"
    (with-redefs [presentations-db/delete-presentation (spy/stub 1)]
      (let [app      (test-app/api-routes-with-auth)
            response (app (-> (mock/request :delete (str "/api/presentations/" test-presentation-id))
                              (mock/header :authorization (td/test-auth-token #{:admin}))))]
        (is (= 204 (:status response)))
        (is (empty? (:body response)))
        (is (spy/called-once-with? presentations-db/delete-presentation nil test-presentation-id)))))

  (doseq [role [:moderator :guest]]
    (testing (str "Test DELETE /presentations/:presentation-id authorized with " role " role and valid `presentation-id`")
      (with-redefs [presentations-db/delete-presentation (spy/spy)]
        (let [app      (test-app/api-routes-with-auth)
              response (app (-> (mock/request :delete (str "/api/presentations/" test-presentation-id))
                                (mock/header :authorization (td/test-auth-token #{role}))))
              body     (test-app/parse-body (:body response))]
          (is (= 403 (:status response)))
          (is (= {:message const/no-access-error-message} body))
          (is (spy/not-called? presentations-db/delete-presentation))))))

  (testing "Test DELETE /presentations/:presentation-id without authorization header"
    (with-redefs [presentations-db/delete-presentation (spy/spy)]
      (let [app      (test-app/api-routes-with-auth)
            response (app (mock/request :delete (str "/api/presentations/" test-presentation-id)))
            body     (test-app/parse-body (:body response))]
        (is (= 401 (:status response)))
        (is (= {:message const/not-authorized-error-message} body))
        (is (spy/not-called? presentations-db/delete-presentation)))))

  (testing "Test DELETE /presentations/:presentation-id for non-existing `presentation-id`"
    (with-redefs [presentations-db/delete-presentation (spy/stub 0)]
      (let [app      (test-app/api-routes-with-auth)
            response (app (-> (mock/request :delete (str "/api/presentations/" test-presentation-id))
                              (mock/header :authorization (td/test-auth-token #{:admin}))))
            body     (test-app/parse-body (:body response))]
        (is (= 404 (:status response)))
        (is (= {:message const/not-found-error-message} body))
        (is (spy/called-once-with? presentations-db/delete-presentation nil test-presentation-id)))))

  (testing "Test DELETE /presentations/:presentation-id with invalid `presentation-id`"
    (with-redefs [presentations-db/delete-presentation (spy/spy)]
      (let [app      (test-app/api-routes-with-auth)
            response (app (-> (mock/request :delete "/api/presentations/invalid")
                              (mock/header :authorization (td/test-auth-token #{:admin}))))
            body     (test-app/parse-body (:body response))]
        (is (= 400 (:status response)))
        (is (= {:message const/bad-request-error-message} (dissoc body :details)))
        (is (spy/not-called? presentations-db/delete-presentation)))))

  (testing "Test DELETE /presentations/:presentation-id unexpected result from database"
    (with-redefs [presentations-db/delete-presentation (spy/stub 2)]
      (let [app      (test-app/api-routes-with-auth)
            response (app (-> (mock/request :delete (str "/api/presentations/" test-presentation-id))
                              (mock/header :authorization (td/test-auth-token #{:admin}))))
            body     (test-app/parse-body (:body response))]
        (is (= 500 (:status response)))
        (is (= {:message const/server-error-message} body))
        (is (spy/called-once-with? presentations-db/delete-presentation nil test-presentation-id))))))
