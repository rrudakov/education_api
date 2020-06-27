(ns education.database.articles-test
  (:require [clojure.test :refer [deftest is testing]]
            [education.database.articles :as sut]
            [education.test-data :as td]
            [next.jdbc.sql :as sql]
            [spy.core :as spy])
  (:import java.time.Instant))

(deftest add-article-test
  (testing "Test add article successfully"
    (with-redefs [sql/insert! (spy/stub td/db-test-article)]
      (let [user   (td/auth-user-deserialized-with-role :admin)
            result (sut/add-article nil user td/add-article-request)]
        (is (= (:articles/id td/db-test-article) result))
        (is (spy/called-once-with? sql/insert!
                                   nil
                                   :articles
                                   {:user_id          (:id user)
                                    :title            (:title td/add-article-request)
                                    :body             (:body td/add-article-request)
                                    :featured_image   (:featured_image td/add-article-request)
                                    :description      "No description..."
                                    :is_main_featured false})))))

  (testing "Test create article with provided description and is_main_featured"
    (with-redefs [sql/insert! (spy/stub td/db-test-article)]
      (let [description      "Some custom description"
            is-main-featured true
            request          (-> td/add-article-request
                                 (assoc :description description)
                                 (assoc :is_main_featured is-main-featured))
            user             (td/auth-user-deserialized-with-role :admin)
            _                (sut/add-article nil user request)]
        (is (spy/called-once-with? sql/insert!
                                   nil
                                   :articles
                                   {:user_id          (:id user)
                                    :title            (:title td/add-article-request)
                                    :body             (:body td/add-article-request)
                                    :featured_image   (:featured_image td/add-article-request)
                                    :description      description
                                    :is_main_featured is-main-featured}))))))

(deftest update-article-test
  (testing "Test update article database query"
    (with-redefs [sql/update! (spy/stub {:next.jdbc/update-count 1})]
      (let [now                    (Instant/now)
            result                 (sut/update-article nil (:articles/id td/db-test-article) td/add-article-request)
            [[_ table query opts]] (spy/calls sql/update!)]
        (is (= {:title          (:title td/add-article-request)
                :body           (:body td/add-article-request)
                :featured_image (:featured_image td/add-article-request)
                :updated_on     now}
               (assoc query :updated_on now)))
        (is (= :articles table))
        (is (= {:id (:articles/id td/db-test-article)} opts))
        (is (= 1 result)))))

  (testing "Test update article with empty body"
    (with-redefs [sql/update! (spy/stub {:next.jdbc/update-count 1})]
      (let [article-id             42
            _                      (sut/update-article nil article-id {})
            [[_ table query opts]] (spy/calls sql/update!)]
        (is (= (list :updated_on) (keys query)))
        (is (= :articles table))
        (is (= {:id article-id} opts))))))

(def get-all-articles-query
  "Expected raw SQL query for fetching all articles from database."
  (str "SELECT id, user_id, title, featured_image, updated_on, description "
       "FROM articles ORDER BY updated_on DESC LIMIT ?"))

(deftest get-all-articles-test
  (testing "Test get all articles default database query"
    (with-redefs [sql/query (spy/spy)]
      (sut/get-all-articles nil)
      (is (spy/called-once-with? sql/query nil [get-all-articles-query 100]))))

  (testing "Test get all articles with custom limit param"
    (with-redefs [sql/query (spy/spy)]
      (let [limit-param 32
            _           (sut/get-all-articles nil limit-param)]
        (is (spy/called-once-with? sql/query nil [get-all-articles-query limit-param]))))))

(deftest get-latest-full-sized-articles-query
  (testing "Test get latest full sized articles database query"
    (with-redefs [sql/query (spy/mock (fn [_ q] q))]
      (let [number-param   30
            [query number] (sut/get-latest-full-sized-articles nil number-param)]
        (is (= (str "SELECT id, user_id, title, body, featured_image, created_on, updated_on, description "
                    "FROM articles ORDER BY updated_on DESC LIMIT ?") query))
        (is (= number-param number ))))))

(deftest get-user-articles-test-default-query
  (testing "Test get user articles default database query"
    (with-redefs [sql/query (spy/mock (fn [_ q] q))]
      (let [user-id-param         42
            [query user-id limit] (sut/get-user-articles nil user-id-param)]
        (is (= (str "SELECT id, user_id, title, featured_image, updated_on, description "
                    "FROM articles "
                    "WHERE user_id = ? ORDER BY updated_on DESC LIMIT ?") query))
        (is (= limit 100))
        (is (= user-id user-id-param))))))

(deftest get-user-articles-test-query-with-limit
  (testing "Test get user articles with custom limit param"
    (with-redefs [sql/query (spy/mock (fn [_ q] q))]
      (let [limit-param 32
            [_ _ limit] (sut/get-user-articles nil 1 limit-param)]
        (is (= limit-param limit))))))

(deftest get-article-by-id-test-table-name
  (testing "Test get article by ID database table name"
    (with-redefs [sql/get-by-id (spy/mock (fn [_ t _] t))]
      (is (= :articles (sut/get-article-by-id nil nil))))))

(deftest get-article-by-id-test-article-id-param
  (testing "Test get article by ID article-id param"
    (with-redefs [sql/get-by-id (spy/mock (fn [_ _ i] i))]
      (let [article-id-param 42]
        (is (= article-id-param (sut/get-article-by-id nil article-id-param)))))))

(deftest get-last-featured-article-test-query
  (testing "Get last featured article database query"
    (with-redefs [sql/query (spy/spy)]
      (sut/get-last-featured-article nil)
      (let [[[_ [query limit]]] (spy/calls sql/query)]
        (is (= (str "SELECT id, user_id, title, featured_image, updated_on, description "
                    "FROM articles "
                    "WHERE is_main_featured = TRUE "
                    "ORDER BY updated_on DESC LIMIT ?") query))
        (is (= 1 limit))))))

(deftest get-last-featured-article-test-no-results
  (testing "Get last featured article no results from database"
    (with-redefs [sql/query (spy/stub [])]
      (is (= nil (sut/get-last-featured-article nil))))))

(deftest get-last-featured-article-test-first-result
  (testing "Get last featured article should take first result from database"
    (let [res [1 2 3 4 5 6]]
      (with-redefs [sql/query (spy/stub res)]
        (is (= (first res) (sut/get-last-featured-article nil)))))))

(deftest get-last-featured-articles-test-query
  (testing "Get latest featured article list database query"
    (with-redefs [sql/query (spy/spy)]
      (let [limit-param                3
            _                          (sut/get-last-featured-articles nil limit-param)
            [[_ [query limit offset]]] (spy/calls sql/query)]
        (is (= limit-param limit))
        (is (= 1 offset))
        (is (= (str "SELECT id, user_id, title, featured_image, updated_on, description "
                    "FROM articles "
                    "WHERE is_main_featured = TRUE "
                    "ORDER BY updated_on DESC LIMIT ? OFFSET ?") query))))))

(deftest get-last-featured-articles-test-no-results
  (testing "Get latest featured article list no results from database"
    (with-redefs [sql/query (spy/stub [])]
      (let [res (sut/get-last-featured-articles nil 3)]
        (is (= [] res))))))

(deftest delete-article-test-table-name
  (testing "Delete article database table name"
    (with-redefs [sql/delete! (spy/mock (fn [_ t _] t))]
      (is (= :articles (sut/delete-article nil nil))))))

(deftest delete-article-test-article-id-param
  (testing "Delete article article-id param"
    (with-redefs [sql/delete! (spy/mock (fn [_ _ i] i))]
      (let [article-id 42]
        (is (= {:id article-id} (sut/delete-article nil article-id)))))))

(deftest can-update-test-success-role
  (doseq [role [:moderator :admin]]
    (testing "Test can-update? returns true for moderator and admin roles"
      (with-redefs [sql/get-by-id (spy/spy)]
        (let [user (->> role name vector (assoc td/auth-user-deserialized :roles))]
          (is (= true (sut/can-update? nil user nil)))
          (is (spy/not-called? sql/get-by-id)))))))

(deftest can-update-test-article-owner
  (testing "Test can-update? returns true for article owner"
    (let [role       :guest
          user       (->> role name vector (assoc td/auth-user-deserialized :roles))
          user-id    (:id user)
          article-id 44]
      (with-redefs [sql/get-by-id (spy/stub {:articles/user_id user-id})]
        (is (= true (sut/can-update? nil user article-id)))
        (is (spy/called-once-with? sql/get-by-id nil :articles article-id))))))

(deftest can-update-test-guest-not-owner
  (testing "Test can-update? returns false for guest and not article owner"
    (let [role            :guest
          user            (->> role name vector (assoc td/auth-user-deserialized :roles))
          article-user-id (inc (:id user))]
      (with-redefs [sql/get-by-id (spy/stub {:articles/user_id article-user-id})]
        (is (= false (sut/can-update? nil user nil)))
        (is (spy/called-once-with? sql/get-by-id nil :articles nil))))))
