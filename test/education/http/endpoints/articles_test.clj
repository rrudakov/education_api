(ns education.http.endpoints.articles-test
  (:require [cheshire.core :as cheshire]
            [clojure.test :as t]
            [education.database.articles :as articlesdb]
            [education.http.routes :as r]
            [ring.mock.request :as mock]
            [spy.core :as spy]))

(def test-db-full-article
  "Created article."
  {:articles/id               22
   :articles/user_id          1
   :articles/title            "Article title"
   :articles/body             "Article body"
   :articles/featured_image   "https://some.featured.image.png"
   :articles/description      "Test description"
   :articles/is_main_featured false
   :articles/created_on       #inst "2020-05-27T21:17:18Z"
   :articles/updated_on       #inst "2020-05-27T21:17:18Z"})

(defn- parse-body
  [body]
  (cheshire/parse-string (slurp body) true))

(t/deftest get-articles-test
  (t/testing "Test GET request to /articles endpoint"
    (with-redefs [articlesdb/get-all-articles (spy/mock (fn [_ _] [test-db-full-article]))]
      (let [db (spy/spy)
            app (r/api-routes {:datasource db} {})
            response (app (-> (mock/request :get "/api/articles")))
            body (parse-body (:body response))]
        (t/is (= 200 (:status response)))
        (t/is (= (list
                  {:id 22,
                   :user_id 1,
                   :title "Article title",
                   :featured_image "https://some.featured.image.png",
                   :updated_on "2020-05-27T21:17:18Z",
                   :description "Test description"})
                 body))))))
