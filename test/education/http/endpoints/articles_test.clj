(ns education.http.endpoints.articles-test
  (:require [cheshire.core :as cheshire]
            [clojure.test :refer :all]
            [education.database.articles :as articlesdb]
            [education.http.constants :refer :all]
            [education.http.endpoints.test-app :refer [test-api-routes-with-auth]]
            [education.http.routes :as r]
            [education.test-data :refer :all]
            [ring.mock.request :as mock]
            [spy.core :as spy])
  (:import java.time.Instant))

(def default-description
  "Default description in case database value is empty."
  "No description...")

(def default-limit
  "Default limit value in case no query parameters specified."
  100)

(def test-db-full-article-1
  "Created article."
  {:articles/id               22
   :articles/user_id          1
   :articles/title            "Article title"
   :articles/body             "Article body"
   :articles/featured_image   "https://some.featured.image.png"
   :articles/description      "Test description"
   :articles/is_main_featured false
   :articles/created_on       (Instant/parse "2020-05-27T21:17:18Z")
   :articles/updated_on       (Instant/parse "2020-05-27T21:17:18Z")})

(def test-db-full-article-2
  "Created article."
  {:articles/id               23
   :articles/user_id          3
   :articles/title            "Article title 2"
   :articles/body             "Article body 2"
   :articles/featured_image   "https://some.featured.image2.png"
   :articles/description      "Test description 2"
   :articles/is_main_featured false
   :articles/created_on       (Instant/parse "2020-05-27T22:17:18Z")
   :articles/updated_on       (Instant/parse "2020-05-27T23:17:18Z")})

(def test-response-short-article-1
  "Expected short article response."
  {:id             (:articles/id test-db-full-article-1)
   :user_id        (:articles/user_id test-db-full-article-1)
   :title          (:articles/title test-db-full-article-1)
   :featured_image (:articles/featured_image test-db-full-article-1)
   :updated_on     (str (:articles/updated_on test-db-full-article-1))
   :description    (:articles/description test-db-full-article-1)})

(def test-response-short-article-2
  "Expected short article response."
  {:id             (:articles/id test-db-full-article-2)
   :user_id        (:articles/user_id test-db-full-article-2)
   :title          (:articles/title test-db-full-article-2)
   :featured_image (:articles/featured_image test-db-full-article-2)
   :updated_on     (str (:articles/updated_on test-db-full-article-2))
   :description    (:articles/description test-db-full-article-2)})

(def test-response-full-article-1
  "Expected full article response."
  {:id             (:articles/id test-db-full-article-1)
   :user_id        (:articles/user_id test-db-full-article-1)
   :title          (:articles/title test-db-full-article-1)
   :description    (:articles/description test-db-full-article-1)
   :body           (:articles/body test-db-full-article-1)
   :featured_image (:articles/featured_image test-db-full-article-1)
   :created_on     (str (:articles/created_on test-db-full-article-1))
   :updated_on     (str (:articles/updated_on test-db-full-article-1))})

(def test-response-full-article-2
  "Expected full article response."
  {:id             (:articles/id test-db-full-article-2)
   :user_id        (:articles/user_id test-db-full-article-2)
   :title          (:articles/title test-db-full-article-2)
   :description    (:articles/description test-db-full-article-2)
   :body           (:articles/body test-db-full-article-2)
   :featured_image (:articles/featured_image test-db-full-article-2)
   :created_on     (str (:articles/created_on test-db-full-article-2))
   :updated_on     (str (:articles/updated_on test-db-full-article-2))})

(def test-article-request-valid
  "Valid article create request."
  {:title            "Some title"
   :body             "Some body"
   :featured_image   "http://image"
   :is_main_featured true})

(defn- parse-body
  [body]
  (cheshire/parse-string (slurp body) true))

(deftest create-article-test-success
  (testing "Test POST /articles authorized with valid request body"
    (let [new-article-id 54]
      (with-redefs [articlesdb/add-article (spy/mock (fn [_ u a] new-article-id))]
        (let [role          :guest
              user-expected (assoc auth-user-deserialized :roles (vector (name role)))
              app           (test-api-routes-with-auth (spy/spy))
              response      (app (-> (mock/request :post "/api/articles")
                                     (mock/content-type "application/json")
                                     (mock/header :authorization (test-auth-token #{role}))
                                     (mock/body (cheshire/generate-string test-article-request-valid))))
              body          (parse-body (:body response))]
         (is (= 201 (:status response)))
         (is (= {:id (str new-article-id)} body))
         (is (spy/called-once-with?  articlesdb/add-article nil user-expected test-article-request-valid)))))))

(deftest create-article-test-not-authorized
  (testing "Test POST /articles without authorization header"
    (with-redefs [articlesdb/add-article (spy/spy)]
      (let [app      (test-api-routes-with-auth (spy/spy))
            response (app (-> (mock/request :post "/api/articles")
                              (mock/content-type "application/json")
                              (mock/body (cheshire/generate-string test-article-request-valid))))
            body     (parse-body (:body response))]
        (is (= 401 (:status response)))
        (is (spy/not-called? articlesdb/add-article))
        (is (= {:message not-authorized-error-message} body))))))

(deftest create-article-test-request-body-validation
  (testing "Test POST /articles authorized bad requests"
    (with-redefs [articlesdb/add-article (spy/spy)]
      (let [app (test-api-routes-with-auth (spy/spy))
            req #(-> (mock/request :post "/api/articles")
                     (mock/content-type "application/json")
                     (mock/header :authorization (test-auth-token #{:moderator}))
                     (mock/body (cheshire/generate-string %)))]
        (are [status-expected request] (= status-expected (:status (app (req request))))
          400 (dissoc test-article-request-valid :title)
          400 (dissoc test-article-request-valid :body)
          400 (dissoc test-article-request-valid :featured_image)
          400 (dissoc test-article-request-valid :is_main_featured))
        (are [body-expected request] (= body-expected (:message (parse-body (:body (app (req request))))))
          bad-request-error-message (dissoc test-article-request-valid :title)
          bad-request-error-message (dissoc test-article-request-valid :body)
          bad-request-error-message (dissoc test-article-request-valid :featured_image)
          bad-request-error-message (dissoc test-article-request-valid :is_main_featured))))))

(deftest update-article-test-success
  (testing "Test PATCH /article/:id authorized with valid body"
    (with-redefs [articlesdb/update-article (spy/mock (fn [_ _ _] 1))
                  articlesdb/can-update?    (spy/mock (fn [_ _ _] true))]
      (let [article-id 43
            role       :guest
            app        (test-api-routes-with-auth (spy/spy))
            response   (app (-> (mock/request :patch (str "/api/articles/" article-id))
                                (mock/content-type "application/json")
                                (mock/header :authorization (test-auth-token #{role}))
                                (mock/body (cheshire/generate-string test-article-request-valid))))]
        (is (= 204 (:status response)))
        (is (spy/called-once-with? articlesdb/can-update?
                                     nil
                                     (assoc auth-user-deserialized :roles (vector (name role)))
                                     article-id))
        (is (spy/called-once-with? articlesdb/update-article
                                     nil
                                     article-id
                                     test-article-request-valid))))))

(deftest update-article-test-not-authorized
  (testing "Test PATCH /articles/:id without authorization header"
    (with-redefs [articlesdb/update-article (spy/spy)]
      (let [app      (test-api-routes-with-auth (spy/spy))
            response (app (-> (mock/request :patch "/api/articles/43")
                              (mock/content-type "application/json")
                              (mock/body (cheshire/generate-string test-article-request-valid))))
            body     (parse-body (:body response))]
        (is (= 401 (:status response)))
        (is (spy/not-called? articlesdb/update-article))
        (is (= {:message not-authorized-error-message} body))))))

(deftest update-article-test-no-access
  (testing "Test PATCH /articles/:id with guest role"
    (with-redefs [articlesdb/update-article (spy/spy)
                  articlesdb/can-update?    (spy/mock (fn [_ _ _] false))]
      (let [app      (test-api-routes-with-auth (spy/spy))
            response (app (-> (mock/request :patch "/api/articles/44")
                              (mock/content-type "application/json")
                              (mock/header :authorization (test-auth-token #{:guest}))
                              (mock/body (cheshire/generate-string test-article-request-valid))))
            body     (parse-body (:body response))]
        (is (= 403 (:status response)))
        (is (spy/not-called? articlesdb/update-article))
        (is (= {:message no-access-error-message} body))))))

(deftest update-article-test-invalid-id
  (testing "Test PATCH /articles/:id with invalid id"
    (with-redefs [articlesdb/update-article (spy/spy)]
      (let [article-id-param "invalid"
            app              (test-api-routes-with-auth (spy/spy))
            response         (app (-> (mock/request :patch (str "/api/articles/" article-id-param))
                                      (mock/content-type "application/json")
                                      (mock/header :authorization (test-auth-token #{:moderator}))
                                      (mock/body (cheshire/generate-string test-article-request-valid))))
            body             (parse-body (:body response))]
        (is (= 400 (:status response)))
        (is (spy/not-called? articlesdb/update-article))
        (is (contains? body :message))
        (is (= bad-request-error-message (:message body)))))))

(deftest update-article-test-does-not-exist
  (testing "Test PATCH /articles/:id with non-existing article id"
    (with-redefs [articlesdb/update-article (spy/mock (fn [_ _ _] 0))]
      (let [app      (test-api-routes-with-auth (spy/spy))
            response (app (-> (mock/request :patch "/api/articles/456")
                              (mock/content-type "application/json")
                              (mock/header :authorization (test-auth-token #{:admin}))
                              (mock/body (cheshire/generate-string test-article-request-valid))))
            body     (parse-body (:body response))]
        (is (= 404 (:status response)))
        (is (spy/called-once? articlesdb/update-article))
        (is (= {:message not-found-error-message} body))))))

(deftest update-article-test-database-error
  (testing "Test PATCH /articles/:id unexpected response from database"
    (with-redefs [articlesdb/update-article (spy/mock (fn [_ _ _] 2))]
      (let [app      (test-api-routes-with-auth (spy/spy))
            response (app (-> (mock/request :patch "/api/articles/3")
                              (mock/content-type "application/json")
                              (mock/header :authorization (test-auth-token #{:moderator}))
                              (mock/body (cheshire/generate-string test-article-request-valid))))
            body     (parse-body (:body response))]
        (is (= 500 (:status response)))
        (is (spy/called-once? articlesdb/update-article))
        (is (= {:message server-error-message} body))))))

(deftest get-articles-test-response-body-and-default-limit
  (testing "Test GET /articles endpoint response body and default limit parameter"
    (with-redefs [articlesdb/get-all-articles
                  (spy/mock
                   (fn [_ _]
                     [test-db-full-article-1
                      test-db-full-article-2]))]
      (let [app         (test-api-routes-with-auth (spy/spy))
            response    (app (mock/request :get "/api/articles"))
            body        (parse-body (:body response))
            [[_ limit]] (spy/calls articlesdb/get-all-articles)]
        (is (= 200 (:status response)))
        (is (= [test-response-short-article-1 test-response-short-article-2] body))
        (is (= default-limit limit))))))

(deftest get-articles-test-default-description
  (testing "Test GET /articles endpoint return default description"
    (with-redefs [articlesdb/get-all-articles
                  (spy/mock
                   (fn [_ _]
                     [(dissoc test-db-full-article-1 :articles/description)]))]
      (let [app      (test-api-routes-with-auth (spy/spy))
            response (app (-> (mock/request :get "/api/articles")))
            body     (parse-body (:body response))]
        (is (= 200 (:status response)))
        (is (= [(assoc test-response-short-article-1 :description default-description)] body))))))

(deftest get-articles-test-limit-param
  (testing "Test GET /articles endpoint accept optional limit parameter"
    (with-redefs [articlesdb/get-all-articles (spy/mock (fn [_ _] []))]
      (let [limit-param 1
            app         (test-api-routes-with-auth (spy/spy))
            response    (app (-> (mock/request :get (str "/api/articles?limit=" limit-param))))
            body        (parse-body (:body response))
            [[_ limit]] (spy/calls articlesdb/get-all-articles)]
        (is (= 200 (:status response)))
        (is (= limit-param limit))))))

(deftest get-articles-test-user-id-param
  (testing "Test GET /articles endpoint accept optional user_id parameter"
    (with-redefs [articlesdb/get-user-articles (spy/mock (fn [_ _ _] []))
                  articlesdb/get-all-articles  (spy/spy)]
      (let [user-id-param       42
            app                 (test-api-routes-with-auth (spy/spy))
            response            (app (-> (mock/request :get (str "/api/articles?user_id=" user-id-param))))
            body                (parse-body (:body response))
            [[_ user-id limit]] (spy/calls articlesdb/get-user-articles)]
        (is (= 200 (:status response)))
        (is (= user-id-param user-id))
        (is (= default-limit limit))
        (is (spy/not-called? articlesdb/get-all-articles))))))

(deftest get-article-test-invalid-limit-param
  (doseq [url ["/api/articles?limit=invalid"
               "/api/articles?user_id=invalid"]]
    (testing "Test GET /articles endpoint query parameter validation"
      (let [app      (test-api-routes-with-auth (spy/spy))
            response (app (-> (mock/request :get url)))
            body     (parse-body (:body response))]
        (is (= 400 (:status response)))
        (is (contains? body :message))
        (is (contains? body :details))
        (is (= bad-request-error-message (:message body)))))))

(deftest get-latest-articles-test-response-body
  (testing "Test GET /articles/latest with valid limit parameter"
    (with-redefs [articlesdb/get-latest-full-sized-articles
                  (spy/mock
                   (fn [_ _]
                     [test-db-full-article-1
                      test-db-full-article-2]))]
      (let [limit    88
            app      (test-api-routes-with-auth (spy/spy))
            url      (str "/api/articles/latest?limit=" limit)
            response (app (-> (mock/request :get url)))
            body     (parse-body (:body response))]
        (is (= 200 (:status response)))
        (is (spy/called-once-with? articlesdb/get-latest-full-sized-articles nil limit))
        (is (= [test-response-full-article-1 test-response-full-article-2] body))))))

(deftest get-latest-articles-test-no-limit-param
  (doseq [url ["/api/articles/latest"
               "/api/articles/latest?limit=invalid"]]
    (testing "Test GET /articles/latest limit parameter validation"
      (with-redefs [articlesdb/get-latest-full-sized-articles (spy/spy)]
        (let [app      (test-api-routes-with-auth (spy/spy))
              response (app (-> (mock/request :get url)))
              body     (parse-body (:body response))]
          (is (= 400 (:status response)))
          (is (contains? body :message))
          (is (spy/not-called? articlesdb/get-latest-full-sized-articles))
          (is (= bad-request-error-message (:message body))))))))

(deftest get-latest-articles-test-default-description
  (testing "Test GET /articles/latest verify default description"
    (with-redefs [articlesdb/get-latest-full-sized-articles
                  (spy/mock
                   (fn [_ _]
                     [(dissoc test-db-full-article-1 :articles/description)]))]
      (let [app      (test-api-routes-with-auth (spy/spy))
            response (app (-> (mock/request :get "/api/articles/latest?limit=44")))
            body     (parse-body (:body response))]
        (is (= 200 (:status response)))
        (is (spy/called-once? articlesdb/get-latest-full-sized-articles))
        (is (= default-description (:description (first body))))))))

(deftest get-last-featured-articles-test-response-body
  (testing "Test GET /articles/featured/latest with valid request"
    (with-redefs [articlesdb/get-last-featured-articles
                  (spy/mock
                   (fn [_ _]
                     [test-db-full-article-1
                      test-db-full-article-2]))]
      (let [limit    33
            app      (test-api-routes-with-auth (spy/spy))
            response (app (-> (mock/request :get (str "/api/articles/featured/latest?limit=" limit))))
            body     (parse-body (:body response))]
        (is (= 200 (:status response)))
        (is (= [test-response-short-article-1 test-response-short-article-2] body))
        (is (spy/called-once-with? articlesdb/get-last-featured-articles nil limit))))))

(deftest get-last-featured-articles-test-query-validation
  (doseq [url ["/api/articles/featured/latest?limit=invalid"
               "/api/articles/featured/latest"]]
    (testing "Test GET /articles/featured/latest with invalid query parameters"
      (with-redefs [articlesdb/get-last-featured-articles (spy/spy)]
        (let [app      (test-api-routes-with-auth (spy/spy))
              response (app (-> (mock/request :get url)))
              body     (parse-body (:body response))]
          (is (= 400 (:status response)))
          (is (contains? body :message))
          (is (= bad-request-error-message (:message body))))))))
