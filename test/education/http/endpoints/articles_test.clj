(ns education.http.endpoints.articles-test
  (:require [cheshire.core :as cheshire]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [education.database.articles :as articlesdb]
            [education.http.constants :as const]
            [education.http.endpoints.test-app
             :refer
             [api-routes-with-auth parse-body]]
            [education.test-data :as td]
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

(deftest create-article-test
  (testing "Test POST /articles authorized with valid request body"
    (let [new-article-id 54]
      (with-redefs [articlesdb/add-article (spy/stub new-article-id)]
        (let [role          :guest
              user-expected (assoc td/auth-user-deserialized :roles (vector (name role)))
              app           (api-routes-with-auth)
              response      (app (-> (mock/request :post "/api/articles")
                                     (mock/content-type td/application-json)
                                     (mock/header :authorization (td/test-auth-token #{role}))
                                     (mock/body (cheshire/generate-string test-article-request-valid))))
              body          (parse-body (:body response))]
          (is (= 201 (:status response)))
          (is (= {:id new-article-id} body))
          (is (spy/called-once-with? articlesdb/add-article nil user-expected test-article-request-valid))))))

  (testing "Test POST /articles without authorization header"
    (with-redefs [articlesdb/add-article (spy/spy)]
      (let [app      (api-routes-with-auth)
            response (app (-> (mock/request :post "/api/articles")
                              (mock/content-type td/application-json)
                              (mock/body (cheshire/generate-string test-article-request-valid))))
            body     (parse-body (:body response))]
        (is (= 401 (:status response)))
        (is (spy/not-called? articlesdb/add-article))
        (is (= {:message const/not-authorized-error-message} body)))))

  (doseq [[request-body errors] [[(dissoc test-article-request-valid :title)
                                  ["Field title is mandatory"]]
                                 [(dissoc test-article-request-valid :body)
                                  ["Field body is mandatory"]]
                                 [(dissoc test-article-request-valid :featured_image)
                                  ["Field featured_image is mandatory"]]
                                 [(dissoc test-article-request-valid :is_main_featured)
                                  ["Field is_main_featured is mandatory"]]
                                 [(assoc test-article-request-valid :title (str/join (repeat 501 "a")))
                                  ["Title must not be longer than 100 characters"]]
                                 [(assoc test-article-request-valid :title "")
                                  ["Title must not be empty"]]
                                 [(assoc test-article-request-valid :title 123)
                                  ["Title is not valid"]]
                                 [(assoc test-article-request-valid :featured_image (str/join (repeat 501 "a")))
                                  ["Featured_image must not be longer than 500 characters"]]
                                 [(assoc test-article-request-valid :body "")
                                  ["Body must not be empty"]]]]
    (testing "Test POST /articles authorized with invalid request body"
      (with-redefs [articlesdb/add-article (spy/spy)]
        (let [app      (api-routes-with-auth)
              response (app (-> (mock/request :post "/api/articles")
                                (mock/content-type td/application-json)
                                (mock/header :authorization (td/test-auth-token #{:moderator}))
                                (mock/body (cheshire/generate-string request-body))))
              body     (parse-body (:body response))]
          (is (= 400 (:status response)))
          (is (spy/not-called? articlesdb/add-article))
          (is (= {:message const/bad-request-error-message
                  :errors  errors}
                 body)))))))

(deftest update-article-test
  (testing "Test PATCH /article/:id authorized with valid body"
    (with-redefs [articlesdb/update-article (spy/stub 1)
                  articlesdb/can-update?    (spy/stub true)]
      (let [article-id 43
            role       :guest
            app        (api-routes-with-auth)
            response   (app (-> (mock/request :patch (str "/api/articles/" article-id))
                                (mock/content-type td/application-json)
                                (mock/header :authorization (td/test-auth-token #{role}))
                                (mock/body (cheshire/generate-string test-article-request-valid))))]
        (is (= 204 (:status response)))
        (is (spy/called-once-with? articlesdb/can-update?
                                   nil
                                   (td/auth-user-deserialized-with-role role)
                                   article-id))
        (is (spy/called-once-with? articlesdb/update-article
                                   nil
                                   article-id
                                   test-article-request-valid)))))

  (testing "Test PATCH /articles/:id without authorization header"
    (with-redefs [articlesdb/update-article (spy/spy)]
      (let [app      (api-routes-with-auth)
            response (app (-> (mock/request :patch "/api/articles/43")
                              (mock/content-type td/application-json)
                              (mock/body (cheshire/generate-string test-article-request-valid))))
            body     (parse-body (:body response))]
        (is (= 401 (:status response)))
        (is (spy/not-called? articlesdb/update-article))
        (is (= {:message const/not-authorized-error-message} body)))))

  (testing "Test PATCH /articles/:id with guest role"
    (with-redefs [articlesdb/update-article (spy/spy)
                  articlesdb/can-update?    (spy/stub false)]
      (let [app      (api-routes-with-auth)
            response (app (-> (mock/request :patch "/api/articles/44")
                              (mock/content-type td/application-json)
                              (mock/header :authorization (td/test-auth-token #{:guest}))
                              (mock/body (cheshire/generate-string test-article-request-valid))))
            body     (parse-body (:body response))]
        (is (= 403 (:status response)))
        (is (spy/not-called? articlesdb/update-article))
        (is (= {:message const/no-access-error-message} body)))))

  (testing "Test PATCH /articles/:id with invalid id"
    (with-redefs [articlesdb/update-article (spy/spy)]
      (let [article-id-param "invalid"
            app              (api-routes-with-auth)
            response         (app (-> (mock/request :patch (str "/api/articles/" article-id-param))
                                      (mock/content-type td/application-json)
                                      (mock/header :authorization (td/test-auth-token #{:moderator}))
                                      (mock/body (cheshire/generate-string test-article-request-valid))))
            body             (parse-body (:body response))]
        (is (= 400 (:status response)))
        (is (spy/not-called? articlesdb/update-article))
        (is (= {:message const/bad-request-error-message
                :errors ["Field id is mandatory"]}
               body)))))

  (testing "Test PATCH /articles/:id with non-existing article id"
    (with-redefs [articlesdb/update-article (spy/stub 0)]
      (let [app      (api-routes-with-auth)
            response (app (-> (mock/request :patch "/api/articles/456")
                              (mock/content-type td/application-json)
                              (mock/header :authorization (td/test-auth-token #{:admin}))
                              (mock/body (cheshire/generate-string test-article-request-valid))))
            body     (parse-body (:body response))]
        (is (= 404 (:status response)))
        (is (spy/called-once? articlesdb/update-article))
        (is (= {:message const/not-found-error-message} body)))))

  (testing "Test PATCH /articles/:id unexpected response from database"
    (with-redefs [articlesdb/update-article (spy/stub 2)]
      (let [app      (api-routes-with-auth)
            response (app (-> (mock/request :patch "/api/articles/3")
                              (mock/content-type td/application-json)
                              (mock/header :authorization (td/test-auth-token #{:moderator}))
                              (mock/body (cheshire/generate-string test-article-request-valid))))
            body     (parse-body (:body response))]
        (is (= 500 (:status response)))
        (is (spy/called-once? articlesdb/update-article))
        (is (= {:message const/server-error-message} body))))))

(deftest get-articles-test
  (testing "Test GET /articles endpoint response body and default limit parameter"
    (with-redefs [articlesdb/get-all-articles (spy/stub [test-db-full-article-1
                                                         test-db-full-article-2])]
      (let [app         (api-routes-with-auth)
            response    (app (mock/request :get "/api/articles"))
            body        (parse-body (:body response))
            [[_ limit]] (spy/calls articlesdb/get-all-articles)]
        (is (= 200 (:status response)))
        (is (= [test-response-short-article-1 test-response-short-article-2] body))
        (is (= default-limit limit)))))

  (testing "Test GET /articles endpoint return default description"
    (with-redefs [articlesdb/get-all-articles
                  (spy/stub [(dissoc test-db-full-article-1 :articles/description)])]
      (let [app      (api-routes-with-auth)
            response (app (-> (mock/request :get "/api/articles")))
            body     (parse-body (:body response))]
        (is (= 200 (:status response)))
        (is (= [(assoc test-response-short-article-1 :description default-description)] body)))))

  (testing "Test GET /articles endpoint accept optional limit parameter"
    (with-redefs [articlesdb/get-all-articles (spy/stub [])]
      (let [limit-param 1
            app         (api-routes-with-auth)
            response    (app (-> (mock/request :get (str "/api/articles?limit=" limit-param))))
            [[_ limit]] (spy/calls articlesdb/get-all-articles)]
        (is (= 200 (:status response)))
        (is (= limit-param limit)))))

  (testing "Test GET /articles endpoint accept optional user_id parameter"
    (with-redefs [articlesdb/get-user-articles (spy/stub [])
                  articlesdb/get-all-articles  (spy/spy)]
      (let [user-id-param       42
            app                 (api-routes-with-auth)
            response            (app (-> (mock/request :get (str "/api/articles?user_id=" user-id-param))))
            [[_ user-id limit]] (spy/calls articlesdb/get-user-articles)]
        (is (= 200 (:status response)))
        (is (= user-id-param user-id))
        (is (= default-limit limit))
        (is (spy/not-called? articlesdb/get-all-articles)))))

  (doseq [url ["/api/articles?limit=invalid"
               "/api/articles?user_id=invalid"]]
    (testing "Test GET /articles endpoint query parameter validation"
      (let [app      (api-routes-with-auth)
            response (app (-> (mock/request :get url)))
            body     (parse-body (:body response))]
        (is (= 400 (:status response)))
        (is (= {:message const/bad-request-error-message
                :errors  ["Value is not valid"]}
               body))))))

(deftest get-latest-articles-test
  (testing "Test GET /articles/latest with valid limit parameter"
    (with-redefs [articlesdb/get-latest-full-sized-articles
                  (spy/stub [test-db-full-article-1 test-db-full-article-2])]
      (let [limit    88
            app      (api-routes-with-auth)
            url      (str "/api/articles/latest?limit=" limit)
            response (app (-> (mock/request :get url)))
            body     (parse-body (:body response))]
        (is (= 200 (:status response)))
        (is (spy/called-once-with? articlesdb/get-latest-full-sized-articles nil limit))
        (is (= [test-response-full-article-1 test-response-full-article-2] body)))))

  (doseq [url ["/api/articles/latest"
               "/api/articles/latest?limit=invalid"]]
    (testing "Test GET /articles/latest limit parameter validation"
      (with-redefs [articlesdb/get-latest-full-sized-articles (spy/spy)]
        (let [app      (api-routes-with-auth)
              response (app (-> (mock/request :get url)))
              body     (parse-body (:body response))]
          (is (= 400 (:status response)))
          (is (spy/not-called? articlesdb/get-latest-full-sized-articles))
          (is (= {:message const/bad-request-error-message
                  :errors  ["Value is not valid"]}
                 body))))))

  (testing "Test GET /articles/latest verify default description"
    (with-redefs [articlesdb/get-latest-full-sized-articles
                  (spy/stub [(dissoc test-db-full-article-1 :articles/description)])]
      (let [app      (api-routes-with-auth)
            response (app (-> (mock/request :get "/api/articles/latest?limit=44")))
            body     (parse-body (:body response))]
        (is (= 200 (:status response)))
        (is (spy/called-once? articlesdb/get-latest-full-sized-articles))
        (is (= default-description (:description (first body))))))))

(deftest get-last-featured-articles-test
  (testing "Test GET /articles/featured/latest with valid request"
    (with-redefs [articlesdb/get-last-featured-articles
                  (spy/stub [test-db-full-article-1 test-db-full-article-2])]
      (let [limit    33
            app      (api-routes-with-auth)
            response (app (-> (mock/request :get (str "/api/articles/featured/latest?limit=" limit))))
            body     (parse-body (:body response))]
        (is (= 200 (:status response)))
        (is (= [test-response-short-article-1 test-response-short-article-2] body))
        (is (spy/called-once-with? articlesdb/get-last-featured-articles nil limit)))))

  (doseq [url ["/api/articles/featured/latest?limit=invalid"
               "/api/articles/featured/latest"]]
    (testing "Test GET /articles/featured/latest with invalid query parameters"
      (with-redefs [articlesdb/get-last-featured-articles (spy/spy)]
        (let [app      (api-routes-with-auth)
              response (app (-> (mock/request :get url)))
              body     (parse-body (:body response))]
          (is (= 400 (:status response)))
          (is (= {:message const/bad-request-error-message
                  :errors  ["Value is not valid"]}
                 body)))))))

(deftest get-article-by-id-test
  (testing "Test GET /articles/:id with valid article-id"
    (with-redefs [articlesdb/get-article-by-id (spy/stub test-db-full-article-1)]
      (let [article-id 99
            app        (api-routes-with-auth)
            response   (app (-> (mock/request :get (str "/api/articles/" article-id))))
            body       (parse-body (:body response))]
        (is (= 200 (:status response)))
        (is (= test-response-full-article-1 body))
        (is (spy/called-once-with? articlesdb/get-article-by-id nil article-id)))))

  (testing "Test GET /articles/:id with non-existing article-id"
    (with-redefs [articlesdb/get-article-by-id (spy/stub nil)]
      (let [app      (api-routes-with-auth)
            response (app (-> (mock/request :get "/api/articles/88")))
            body     (parse-body (:body response))]
        (is (= 404 (:status response)))
        (is (contains? body :message))
        (is (= const/not-found-error-message (:message body)))
        (is (spy/called-once? articlesdb/get-article-by-id)))))

  (testing "Test GET /articles/:id with invalid article id"
    (with-redefs [articlesdb/get-article-by-id (spy/spy)]
      (let [app      (api-routes-with-auth)
            response (app (-> (mock/request :get "/api/articles/invalid")))
            body     (parse-body (:body response))]
        (is (= 400 (:status response)))
        (is (= {:message const/bad-request-error-message
                :errors  ["Value is not valid"]}
               body))
        (is (spy/not-called? articlesdb/get-article-by-id))))))

(deftest get-last-featured-article-test
  (testing "Test GET /articles/featured/main exist response body"
    (with-redefs [articlesdb/get-last-featured-article (spy/stub test-db-full-article-1)]
      (let [app      (api-routes-with-auth)
            response (app (mock/request :get "/api/articles/featured/main"))
            body     (parse-body (:body response))]
        (is (= 200 (:status response)))
        (is (= test-response-short-article-1 body))
        (is (spy/called-once? articlesdb/get-last-featured-article)))))

  (testing "Test GET /articles/featured/main no featured articles in db"
    (with-redefs [articlesdb/get-last-featured-article (spy/stub nil)]
      (let [app      (api-routes-with-auth)
            response (app (mock/request :get "/api/articles/featured/main"))
            body     (parse-body (:body response))]
        (is (= 404 (:status response)))
        (is (contains? body :message))
        (is (= const/not-found-error-message (:message body)))
        (is (spy/called-once? articlesdb/get-last-featured-article))))))

(deftest delete-article-test
  (testing "Test DELETE /articles/:id for existing article-id"
    (with-redefs [articlesdb/delete-article (spy/stub 1)
                  articlesdb/can-update?    (spy/stub true)]
      (let [article-id 532
            role       :guest
            app        (api-routes-with-auth)
            response   (app (-> (mock/request :delete (str "/api/articles/" article-id))
                                (mock/header :authorization (td/test-auth-token #{role}))))]
        (is (= 204 (:status response)))
        (is (spy/called-once-with? articlesdb/can-update?
                                   nil
                                   (td/auth-user-deserialized-with-role role)
                                   article-id))
        (is (spy/called-once-with? articlesdb/delete-article nil article-id)))))

  (testing "Test DELETE /articles/:id with guest role not article owner"
    (with-redefs [articlesdb/delete-article (spy/spy)
                  articlesdb/can-update?    (spy/stub false)]
      (let [article-id 333
            role       :guest
            app        (api-routes-with-auth)
            response   (app (-> (mock/request :delete (str "/api/articles/" article-id))
                                (mock/header :authorization (td/test-auth-token #{role}))))
            body       (parse-body (:body response))]
        (is (= 403 (:status response)))
        (is (contains? body :message))
        (is (= const/no-access-error-message (:message body)))
        (is (spy/called-once-with? articlesdb/can-update?
                                   nil
                                   (td/auth-user-deserialized-with-role role)
                                   article-id))
        (is (spy/not-called? articlesdb/delete-article)))))

  (testing "Test DELETE /articles/:id without authorization token"
    (with-redefs [articlesdb/delete-article (spy/spy)
                  articlesdb/can-update?    (spy/spy)]
      (let [app      (api-routes-with-auth)
            response (app (-> (mock/request :delete "/api/articles/44")))
            body     (parse-body (:body response))]
        (is (= 401 (:status response)))
        (is (contains? body :message))
        (is (= const/not-authorized-error-message (:message body)))
        (is (spy/not-called? articlesdb/can-update?))
        (is (spy/not-called? articlesdb/delete-article)))))

  (testing "Test DELETE /articles/:id for non-existing article-id"
    (with-redefs [articlesdb/delete-article (spy/stub 0)
                  articlesdb/can-update?    (spy/stub true)]
      (let [app      (api-routes-with-auth)
            response (app (-> (mock/request :delete "/api/articles/84")
                              (mock/header :authorization (td/test-auth-token #{:admin}))))
            body     (parse-body (:body response))]
        (is (= 404 (:status response)))
        (is (contains? body :message))
        (is (= const/not-found-error-message (:message body))))))

  (testing "Test DELETE /articles/:id with invalid article-id parameter"
    (with-redefs [articlesdb/delete-article (spy/spy)
                  articlesdb/can-update?    (spy/spy)]
      (let [app      (api-routes-with-auth)
            response (app (-> (mock/request :delete "/api/articles/invalid")
                              (mock/header :authorization (td/test-auth-token #{:moderator}))))
            body     (parse-body (:body response))]
        (is (= 400 (:status response)))
        (is (= {:message const/bad-request-error-message
                :errors  ["Value is not valid"]}
               body))
        (is (spy/not-called? articlesdb/delete-article))
        (is (spy/not-called? articlesdb/can-update?)))))

  (testing "Test DELETE /articles/:id error from database"
    (with-redefs [articlesdb/delete-article (spy/stub 2)
                  articlesdb/can-update?    (spy/stub true)]
      (let [app      (api-routes-with-auth)
            response (app (-> (mock/request :delete "/api/articles/834")
                              (mock/header :authorization (td/test-auth-token #{:admin}))))
            body     (parse-body (:body response))]
        (is (= 500 (:status response)))
        (is (contains? body :message))
        (is (= const/server-error-message (:message body)))))))
