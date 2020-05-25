(ns education.database.articles-test
  (:require [clojure.test :as t]
            [education.database.articles :as sut]
            [next.jdbc.sql :as sql])
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
  {:title "Test title",
   :body "Test body",
   :featured_image "https://featured.image.com/image.png",})

(def test-db-article
  "Created article."
  {:articles/id 22
   :articles/user_id (:id test-user)
   :articles/title (:title test-request-article)
   :articles/body (:body test-request-article)
   :articles/description "Test description"
   :articles/is_main_featured false
   :articles/created_on (Instant/now)
   :articles/updated_on (Instant/now)})

(t/deftest add-article-test-article-id
  (with-redefs [sql/insert! (fn [_ _ _] test-db-article)]
    (t/testing "Test return new article ID"
      (t/is (= (:articles/id test-db-article)
               (sut/add-article nil test-user test-request-article))))))

(t/deftest add-article-test-db-query
  (let [query (atom {})]
    (with-redefs [sql/insert! (fn [_ _ q] (do (reset! query q) test-db-article))]
      (t/testing "Test create article database query"
        (t/is (= {:user_id (:id test-user)
                  :title (:title test-request-article)
                  :body (:body test-request-article)
                  :featured_image (:featured_image test-request-article)
                  :description "No description..."
                  :is_main_featured false}
                 (do
                   (sut/add-article nil test-user test-request-article)
                   @query)))))))

(t/deftest add-article-test-table-name
  (let [table (atom nil)]
    (with-redefs [sql/insert! (fn [_ t _] (do (reset! table t) test-db-article))]
      (t/testing "test create article database name"
        (t/is (= :articles
                 (do
                   (sut/add-article nil nil nil)
                   @table)))))))

(t/deftest add-article-test-request-with-optional-fields
  (let [description "Some custom description"
        is-main-featured true
        query (atom {})]
    (with-redefs [sql/insert! (fn [_ _ q] (do (reset! query q) test-db-article))]
      (t/testing "Test create article with provided description and is_main_featured"
        (t/is (= {:user_id (:id test-user)
                  :title (:title test-request-article)
                  :body (:body test-request-article)
                  :featured_image (:featured_image test-request-article)
                  :description description
                  :is_main_featured is-main-featured}
                 (do
                   (sut/add-article nil
                                    test-user
                                    (-> test-request-article
                                        (assoc :description description)
                                        (assoc :is_main_featured is-main-featured)))
                   @query)))))))

(t/deftest update-article-test-db-query
  (let [now (Instant/now)]
    (with-redefs [sql/update! (fn [_ _ q _] q)]
      (t/testing "Test update article database query"
        (t/is (= {:title (:title test-request-article)
                  :body (:body test-request-article)
                  :featured_image (:featured_image test-request-article)
                  :updated_on now}
                 (-> (sut/update-article nil (:articles/id test-db-article) test-request-article)
                     (assoc :updated_on now))))))))

(t/deftest update-article-test-table-name
  (with-redefs [sql/update! (fn [_ t _ _] t)]
    (t/testing "Test update article database table name"
      (t/is (= :articles
               (sut/update-article nil nil nil))))))

(t/deftest update-article-test-empty-body
  (with-redefs [sql/update! (fn [_ _ q _] q)]
    (t/testing "Test update article with empty body"
      (t/is (= (list :updated_on)
               (keys (sut/update-article nil nil {})))))))

(t/deftest update-article-test-article-id
  (let [article-id 432]
    (with-redefs [sql/update! (fn [_ _ _ i] i)]
      (t/testing "Test update article database article ID"
        (t/is (= {:id article-id}
                 (sut/update-article nil article-id nil)))))))
