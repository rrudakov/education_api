(ns education.http.endpoints.articles-test
  (:require [cheshire.core :as cheshire]
            [clojure.test :as t]
            [education.database.articles :as articlesdb]
            [education.http.routes :as r]
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

(defn- parse-body
  [body]
  (cheshire/parse-string (slurp body) true))

;; (t/deftest create-article-test-success
;;   (t/testing "Test POST /articles with valid request body"
;;     (with-redefs [articlesdb/add-article (spy/mock (fn [_ u a] 54))]
;;       (let [db       (spy/spy)
;;             app      (r/api-routes db nil)
;;             response (app (-> (mock/request :post "/api/articles")
;;                               (mock/content-type "application/json")
;;                               (mock/body (cheshire/generate-string {:title "Some title"}))))]
;;         (t/is (= 201 (:status response)))))))

(t/deftest get-articles-test-response-body-and-default-limit
  (t/testing "Test GET /articles endpoint response body and default limit parameter"
    (with-redefs [articlesdb/get-all-articles (spy/mock
                                               (fn [_ _]
                                                 [test-db-full-article-1
                                                  test-db-full-article-2]))]
      (let [db          (spy/spy)
            app         (r/api-routes db nil)
            response    (app (-> (mock/request :get "/api/articles")))
            body        (parse-body (:body response))
            [[_ limit]] (spy/calls articlesdb/get-all-articles)]
        (t/is (= 200 (:status response)))
        (t/is (= (list
                  {:id             (:articles/id test-db-full-article-1)
                   :user_id        (:articles/user_id test-db-full-article-1)
                   :title          (:articles/title test-db-full-article-1)
                   :featured_image (:articles/featured_image test-db-full-article-1)
                   :updated_on     (str (:articles/updated_on test-db-full-article-1))
                   :description    (:articles/description test-db-full-article-1)}
                  {:id             (:articles/id test-db-full-article-2)
                   :user_id        (:articles/user_id test-db-full-article-2)
                   :title          (:articles/title test-db-full-article-2)
                   :featured_image (:articles/featured_image test-db-full-article-2)
                   :updated_on     (str (:articles/updated_on test-db-full-article-2))
                   :description    (:articles/description test-db-full-article-2)})
                 body))
        (t/is (= default-limit limit))))))

(t/deftest get-articles-test-default-description
  (t/testing "Test GET /articles endpoint return default description"
    (with-redefs [articlesdb/get-all-articles (spy/mock
                                               (fn [_ _]
                                                 [(dissoc test-db-full-article-1 :articles/description)]))]
      (let [db       (spy/spy)
            app      (r/api-routes db nil)
            response (app (-> (mock/request :get "/api/articles")))
            body     (parse-body (:body response))]
        (t/is (= 200 (:status response)))
        (t/is (= (list {:id             (:articles/id test-db-full-article-1)
                        :user_id        (:articles/user_id test-db-full-article-1)
                        :title          (:articles/title test-db-full-article-1)
                        :featured_image (:articles/featured_image test-db-full-article-1)
                        :updated_on     (str (:articles/updated_on test-db-full-article-1))
                        :description    default-description})
                 body))))))

(t/deftest get-articles-test-limit-param
  (t/testing "Test GET /articles endpoint accept optional limit parameter"
    (with-redefs [articlesdb/get-all-articles (spy/mock (fn [_ _] []))]
      (let [db          (spy/spy)
            limit-param 1
            app         (r/api-routes db nil)
            response    (app (-> (mock/request :get (str "/api/articles?limit=" limit-param))))
            body        (parse-body (:body response))
            [[_ limit]] (spy/calls articlesdb/get-all-articles)]
        (t/is (= 200 (:status response)))
        (t/is (= limit-param limit))))))

(t/deftest get-articles-test-user-id-param
  (t/testing "Test GET /articles endpoint accept optional user_id parameter"
    (with-redefs [articlesdb/get-user-articles (spy/mock (fn [_ _ _] []))
                  articlesdb/get-all-articles  (spy/spy)]
      (let [db                  (spy/spy)
            user-id-param       42
            app                 (r/api-routes db nil)
            response            (app (-> (mock/request :get (str "/api/articles?user_id=" user-id-param))))
            body                (parse-body (:body response))
            [[_ user-id limit]] (spy/calls articlesdb/get-user-articles)]
        (t/is (= 200 (:status response)))
        (t/is (= user-id-param user-id))
        (t/is (= default-limit limit))
        (t/is (spy/called-n-times? articlesdb/get-all-articles 0))))))

(t/deftest get-article-test-invalid-limit-param
  (t/testing "Test GET /articles endpoint limit parameter validation"
    (let [db          (spy/spy)
          limit-param "invalid-string"
          app         (r/api-routes db nil)
          response    (app (-> (mock/request :get (str "/api/articles?limit=" limit-param))))
          body        (parse-body (:body response))]
      (t/is (= 400 (:status response)))
      (t/is (contains? body :message))
      (t/is (contains? body :details))
      (t/is (= "Please check request data" (:message body))))))
