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
    (let [updated-rows 3]
      (with-redefs [sql/update! (spy/stub {:next.jdbc/update-count updated-rows})]
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
          (is (= updated-rows result))))))

  (testing "Test update article with empty body"
    (let [updated-rows 2]
      (with-redefs [sql/update! (spy/stub {:next.jdbc/update-count updated-rows})]
        (let [article-id             42
              result                 (sut/update-article nil article-id {})
              [[_ table query opts]] (spy/calls sql/update!)]
          (is (= (list :updated_on) (keys query)))
          (is (= :articles table))
          (is (= {:id article-id} opts))
          (is (= updated-rows result)))))))

(def get-all-articles-query
  "Expected raw SQL query for fetching all articles from database."
  (str "SELECT id, user_id, title, featured_image, updated_on, description "
       "FROM articles ORDER BY updated_on DESC LIMIT ?"))

(deftest get-all-articles-test
  (testing "Test get all articles default database query"
    (with-redefs [sql/query (spy/stub [td/db-test-article])]
      (is (= [td/db-test-article] (sut/get-all-articles nil)))
      (is (spy/called-once-with? sql/query nil [get-all-articles-query 100]))))

  (testing "Test get all articles with custom limit param"
    (with-redefs [sql/query (spy/stub [td/db-test-article])]
      (let [limit-param 32]
        (is (= [td/db-test-article] (sut/get-all-articles nil limit-param)))
        (is (spy/called-once-with? sql/query nil [get-all-articles-query limit-param]))))))

(def get-latest-articles-query
  "Expected raw SQL query for fetching latest full sized articles from database."
  (str "SELECT id, user_id, title, body, featured_image, created_on, updated_on, description "
       "FROM articles ORDER BY updated_on DESC LIMIT ?"))

(deftest get-latest-full-sized-articles-test
  (testing "Test get latest full sized articles database query"
    (with-redefs [sql/query (spy/stub [td/db-test-article])]
      (let [number-param 30]
        (is (= [td/db-test-article] (sut/get-latest-full-sized-articles nil number-param)))
        (is (spy/called-once-with? sql/query nil [get-latest-articles-query number-param]))))))

(def get-user-articles-query
  "Expected raw SQL query for fetching user articles from database."
  (str "SELECT id, user_id, title, featured_image, updated_on, description "
       "FROM articles "
       "WHERE user_id = ? ORDER BY updated_on DESC LIMIT ?"))

(deftest get-user-articles-test
  (testing "Test get user articles default database query"
    (with-redefs [sql/query (spy/stub [td/db-test-article])]
      (let [user-id-param 42]
        (is (= [td/db-test-article] (sut/get-user-articles nil user-id-param)))
        (is (spy/called-once-with? sql/query nil [get-user-articles-query user-id-param 100])))))

  (testing "Test get user articles with custom limit parameter"
    (with-redefs [sql/query (spy/stub [td/db-test-article])]
      (let [user-id-param 1
            limit-param   32]
        (is (= [td/db-test-article] (sut/get-user-articles nil user-id-param limit-param)))
        (is (spy/called-once-with? sql/query nil [get-user-articles-query user-id-param limit-param]))))))

(deftest get-article-by-id-test
  (testing "Test get article by ID database table name"
    (with-redefs [sql/get-by-id (spy/stub td/db-test-article)]
      (let [article-id 43]
        (is (= td/db-test-article (sut/get-article-by-id nil article-id)))
        (is (spy/called-once-with? sql/get-by-id nil :articles article-id))))))

(def get-last-featured-article-query
  "Expected raw SQL query for fetching last featured article from database."
  (str "SELECT id, user_id, title, featured_image, updated_on, description "
       "FROM articles "
       "WHERE is_main_featured = TRUE "
       "ORDER BY updated_on DESC LIMIT ?"))

(deftest get-last-featured-article-test
  (testing "Get last featured article database query"
    (with-redefs [sql/query (spy/stub [td/db-test-article])]
      (is (= td/db-test-article (sut/get-last-featured-article nil)))
      (is (spy/called-once-with? sql/query nil [get-last-featured-article-query 1]))))

  (testing "Get last featured article no results from database"
    (with-redefs [sql/query (spy/stub [])]
      (is (= nil (sut/get-last-featured-article nil)))))

  (testing "Get last featured article should take first result from database"
    (let [res [1 2 3 4 5 6]]
      (with-redefs [sql/query (spy/stub res)]
        (is (= (first res) (sut/get-last-featured-article nil)))))))

(def get-last-featured-articles-query
  "Expected raw SQL query for fetching last featured articles from database."
  (str "SELECT id, user_id, title, featured_image, updated_on, description "
       "FROM articles "
       "WHERE is_main_featured = TRUE "
       "ORDER BY updated_on DESC LIMIT ? OFFSET ?"))

(deftest get-last-featured-articles-test
  (testing "Get latest featured article list database query"
    (with-redefs [sql/query (spy/stub [td/db-test-article])]
      (let [limit-param                3]
        (is (= [td/db-test-article] (sut/get-last-featured-articles nil limit-param)))
        (is (spy/called-once-with? sql/query nil [get-last-featured-articles-query limit-param 1])))))

  (testing "Get latest featured article list no results from database"
    (with-redefs [sql/query (spy/stub [])]
      (is (= [] (sut/get-last-featured-articles nil 3))))))

(deftest delete-article-test
  (testing "Delete article database table name"
    (let [deleted-rows 3]
      (with-redefs [sql/delete! (spy/stub {:next.jdbc/update-count deleted-rows})]
        (let [article-id 455]
          (is (= deleted-rows (sut/delete-article nil article-id)))
          (is (spy/called-once-with? sql/delete! nil :articles {:id article-id})))))))

(deftest can-update-test
  (doseq [role [:moderator :admin]]
    (testing "Test can-update? returns true for moderator and admin roles"
      (with-redefs [sql/get-by-id (spy/spy)]
        (let [user (->> role name vector (assoc td/auth-user-deserialized :roles))]
          (is (= true (sut/can-update? nil user nil)))
          (is (spy/not-called? sql/get-by-id))))))

  (testing "Test can-update? returns true for article owner"
    (let [role       :guest
          user       (->> role name vector (assoc td/auth-user-deserialized :roles))
          user-id    (:id user)
          article-id 44]
      (with-redefs [sql/get-by-id (spy/stub {:articles/user_id user-id})]
        (is (= true (sut/can-update? nil user article-id)))
        (is (spy/called-once-with? sql/get-by-id nil :articles article-id)))))

  (testing "Test can-update? returns false for guest and not article owner"
    (let [role            :guest
          user            (->> role name vector (assoc td/auth-user-deserialized :roles))
          article-user-id (inc (:id user))]
      (with-redefs [sql/get-by-id (spy/stub {:articles/user_id article-user-id})]
        (is (= false (sut/can-update? nil user nil)))
        (is (spy/called-once-with? sql/get-by-id nil :articles nil))))))
