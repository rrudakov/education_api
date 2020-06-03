(ns education.database.articles-test
  (:require [clojure.test :as t]
            [education.database.articles :as sut]
            [next.jdbc.sql :as sql]
            [spy.core :as spy])
  (:import java.time.Instant))

(def test-user
  "User for article creation."
  {:id         1
   :username   "rrudakov"
   :email      "rrudakov@pm.me"
   :roles      #{:admin :guest}
   :created_on (Instant/parse "2020-05-13T19:43:07Z")
   :updated_on (Instant/parse "2020-05-13T19:43:07Z")})

(def test-request-article
  "Article to create."
  {:title          "Test title",
   :body           "Test body",
   :featured_image "https://featured.image.com/image.png",})

(def test-db-article
  "Created article."
  {:articles/id               22
   :articles/user_id          (:id test-user)
   :articles/title            (:title test-request-article)
   :articles/body             (:body test-request-article)
   :articles/description      "Test description"
   :articles/is_main_featured false
   :articles/created_on       (Instant/now)
   :articles/updated_on       (Instant/now)})

(t/deftest add-article-test-article-id
  (t/testing "Test return new article ID"
    (with-redefs [sql/insert! (fn [_ _ _] test-db-article)]
      (t/is (= (:articles/id test-db-article)
               (sut/add-article nil test-user test-request-article))))))

(t/deftest add-article-test-db-query
  (t/testing "Test create article database query"
    (with-redefs [sql/insert! (spy/mock (fn [_ _ q] test-db-article))]
      (sut/add-article nil test-user test-request-article)
      (let [[[_ _ query]] (spy/calls sql/insert!)]
        (t/is (= {:user_id          (:id test-user)
                  :title            (:title test-request-article)
                  :body             (:body test-request-article)
                  :featured_image   (:featured_image test-request-article)
                  :description      "No description..."
                  :is_main_featured false}
                 query))))))

(t/deftest add-article-test-table-name
  (t/testing "test create article database name"
    (with-redefs [sql/insert! (spy/mock (fn [_ t _] test-db-article))]
      (sut/add-article nil nil nil)
      (let [[[_ table _]] (spy/calls sql/insert!)]
        (t/is (= :articles table))))))

(t/deftest add-article-test-request-with-optional-fields
  (t/testing "Test create article with provided description and is_main_featured"
    (let [description      "Some custom description"
          is-main-featured true]
      (with-redefs [sql/insert! (spy/mock (fn [_ _ q] test-db-article))]
        (sut/add-article nil
                         test-user
                         (-> test-request-article
                             (assoc :description description)
                             (assoc :is_main_featured is-main-featured)))
        (let [[[_ _ query]] (spy/calls sql/insert!)]
          (t/is (= {:user_id          (:id test-user)
                    :title            (:title test-request-article)
                    :body             (:body test-request-article)
                    :featured_image   (:featured_image test-request-article)
                    :description      description
                    :is_main_featured is-main-featured}
                   query)))))))

(t/deftest update-article-test-db-query
  (t/testing "Test update article database query"
    (let [now (Instant/now)]
      (with-redefs [sql/update! (spy/mock (fn [_ _ q _] {:next.jdbc/update-count 1}))]
        (let [result          (sut/update-article nil (:articles/id test-db-article) test-request-article)
              [[_ _ query _]] (spy/calls sql/update!)]
          (t/is (= {:title          (:title test-request-article)
                    :body           (:body test-request-article)
                    :featured_image (:featured_image test-request-article)
                    :updated_on     now}
                   (assoc query :updated_on now)))
          (t/is (= 1 result)))))))

(t/deftest update-article-test-table-name
  (t/testing "Test update article database table name"
    (with-redefs [sql/update! (spy/mock (fn [_ t _ _] {:next.jdbc/update-count 1}))]
      (let [result (sut/update-article nil nil nil)
            [[_ table _ _]] (spy/calls sql/update!)]
        (t/is (= :articles table))))))

(t/deftest update-article-test-empty-body
  (t/testing "Test update article with empty body"
    (with-redefs [sql/update! (spy/mock (fn [_ _ q _] {:next.jdbc/update-count 1}))]
      (let [result (sut/update-article nil nil {})
            [[_ _ query _]] (spy/calls sql/update!)]
        (t/is (= (list :updated_on) (keys query)))))))

(t/deftest update-article-test-article-id
  (t/testing "Test update article database article ID"
    (let [article-id 432]
      (with-redefs [sql/update! (spy/mock (fn [_ _ _ i] {:next.jdbc/update-count 1}))]
        (let [result (sut/update-article nil article-id nil)
              [[_ _ _ article-id-param]] (spy/calls sql/update!)]
          (t/is (= {:id article-id} article-id-param)))))))

(t/deftest get-all-articles-test-default-query
  (t/testing "Test get all articles default database query"
    (with-redefs [sql/query (fn [_ q] q)]
      (let [[query limit] (sut/get-all-articles nil)]
        (t/is (= "SELECT id, user_id, title, featured_image, updated_on, description FROM articles ORDER BY updated_on DESC LIMIT ?" query))
        (t/is (= 100 limit))))))

(t/deftest get-all-articles-test-query-with-limit
  (t/testing "Test get all articles with custom limit param"
    (with-redefs [sql/query (fn [_ q] q)]
      (let [limit-param 32
            [_ limit]   (sut/get-all-articles nil limit-param)]
        (t/is (= limit-param limit))))))

(t/deftest get-latest-full-sized-articles-query
  (t/testing "Test get latest full sized articles database query"
    (with-redefs [sql/query (fn [_ q] q)]
      (let [number-param 30
            [query number] (sut/get-latest-full-sized-articles nil number-param)]
        (t/is (= "SELECT id, user_id, title, body, featured_image, created_on, updated_on, description FROM articles ORDER BY updated_on DESC LIMIT ?" query))
        (t/is (= number-param number ))))))

(t/deftest get-user-articles-test-default-query
  (t/testing "Test get user articles default database query"
    (with-redefs [sql/query (fn [_ q] q)]
      (let [user-id-param         42
            [query user-id limit] (sut/get-user-articles nil user-id-param)]
        (t/is (= "SELECT id, user_id, title, featured_image, updated_on, description FROM articles WHERE user_id = ? ORDER BY updated_on DESC LIMIT ?" query))
        (t/is (= limit 100))
        (t/is (= user-id user-id-param))))))

(t/deftest get-user-articles-test-query-with-limit
  (t/testing "Test get user articles with custom limit param"
    (with-redefs [sql/query (fn [_ q] q)]
      (let [limit-param 32
            [_ _ limit] (sut/get-user-articles nil 1 limit-param)]
        (t/is (= limit-param limit))))))

(t/deftest get-article-by-id-test-table-name
  (t/testing "Test get article by ID database table name"
    (with-redefs [sql/get-by-id (fn [_ t _] t)]
      (t/is (= :articles (sut/get-article-by-id nil nil))))))

(t/deftest get-article-by-id-test-article-id-param
  (t/testing "Test get article by ID article-id param"
    (with-redefs [sql/get-by-id (fn [_ _ i] i)]
      (let [article-id-param 42]
        (t/is (= article-id-param (sut/get-article-by-id nil article-id-param)))))))

(t/deftest get-last-featured-article-test-query
  (t/testing "Get last featured article database query"
    (with-redefs [sql/query (spy/spy)]
      (sut/get-last-featured-article nil)
      (let [[[_ [query limit]]] (spy/calls sql/query)]
        (t/is (= "SELECT id, user_id, title, featured_image, updated_on, description FROM articles WHERE is_main_featured = TRUE ORDER BY updated_on DESC LIMIT ?" query))
        (t/is (= 1 limit))))))

(t/deftest get-last-featured-article-test-no-results
  (t/testing "Get last featured article no results from database"
    (with-redefs [sql/query (fn [_ _] [])]
      (t/is (= nil (sut/get-last-featured-article nil))))))

(t/deftest get-last-featured-article-test-first-result
  (t/testing "Get last featured article should take first result from database"
    (let [res [1 2 3 4 5 6]]
      (with-redefs [sql/query (fn [_ _] res)]
        (t/is (= (first res) (sut/get-last-featured-article nil)))))))

(t/deftest delete-article-test-table-name
  (t/testing "Delete article database table name"
    (with-redefs [sql/delete! (fn [_ t _] t)]
      (t/is (= :articles (sut/delete-article nil nil))))))

(t/deftest delete-article-test-article-id-param
  (t/testing "Delete article article-id param"
    (with-redefs [sql/delete! (fn [_ _ i] i)]
      (let [article-id 42]
        (t/is (= {:id article-id} (sut/delete-article nil article-id)))))))
