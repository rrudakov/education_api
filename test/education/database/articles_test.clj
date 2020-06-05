(ns education.database.articles-test
  (:require [clojure.test :refer :all]
            [education.database.articles :as sut]
            [education.test-data :refer [auth-user-deserialized]]
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

(deftest add-article-test-article-id
  (testing "Test return new article ID"
    (with-redefs [sql/insert! (fn [_ _ _] test-db-article)]
      (is (= (:articles/id test-db-article)
             (sut/add-article nil test-user test-request-article))))))

(deftest add-article-test-db-query
  (testing "Test create article database query"
    (with-redefs [sql/insert! (spy/mock (fn [_ _ q] test-db-article))]
      (sut/add-article nil test-user test-request-article)
      (let [[[_ _ query]] (spy/calls sql/insert!)]
        (is (= {:user_id          (:id test-user)
                :title            (:title test-request-article)
                :body             (:body test-request-article)
                :featured_image   (:featured_image test-request-article)
                :description      "No description..."
                :is_main_featured false}
               query))))))

(deftest add-article-test-table-name
  (testing "test create article database name"
    (with-redefs [sql/insert! (spy/mock (fn [_ t _] test-db-article))]
      (sut/add-article nil nil nil)
      (let [[[_ table _]] (spy/calls sql/insert!)]
        (is (= :articles table))))))

(deftest add-article-test-request-with-optional-fields
  (testing "Test create article with provided description and is_main_featured"
    (let [description      "Some custom description"
          is-main-featured true]
      (with-redefs [sql/insert! (spy/mock (fn [_ _ q] test-db-article))]
        (sut/add-article nil
                         test-user
                         (-> test-request-article
                             (assoc :description description)
                             (assoc :is_main_featured is-main-featured)))
        (let [[[_ _ query]] (spy/calls sql/insert!)]
          (is (= {:user_id          (:id test-user)
                  :title            (:title test-request-article)
                  :body             (:body test-request-article)
                  :featured_image   (:featured_image test-request-article)
                  :description      description
                  :is_main_featured is-main-featured}
                 query)))))))

(deftest update-article-test-db-query
  (testing "Test update article database query"
    (let [now (Instant/now)]
      (with-redefs [sql/update! (spy/mock (fn [_ _ q _] {:next.jdbc/update-count 1}))]
        (let [result          (sut/update-article nil (:articles/id test-db-article) test-request-article)
              [[_ _ query _]] (spy/calls sql/update!)]
          (is (= {:title          (:title test-request-article)
                  :body           (:body test-request-article)
                  :featured_image (:featured_image test-request-article)
                  :updated_on     now}
                 (assoc query :updated_on now)))
          (is (= 1 result)))))))

(deftest update-article-test-table-name
  (testing "Test update article database table name"
    (with-redefs [sql/update! (spy/mock (fn [_ t _ _] {:next.jdbc/update-count 1}))]
      (let [result          (sut/update-article nil nil nil)
            [[_ table _ _]] (spy/calls sql/update!)]
        (is (= :articles table))))))

(deftest update-article-test-empty-body
  (testing "Test update article with empty body"
    (with-redefs [sql/update! (spy/mock (fn [_ _ q _] {:next.jdbc/update-count 1}))]
      (let [result          (sut/update-article nil nil {})
            [[_ _ query _]] (spy/calls sql/update!)]
        (is (= (list :updated_on) (keys query)))))))

(deftest update-article-test-article-id
  (testing "Test update article database article ID"
    (let [article-id 432]
      (with-redefs [sql/update! (spy/mock (fn [_ _ _ i] {:next.jdbc/update-count 1}))]
        (let [result                     (sut/update-article nil article-id nil)
              [[_ _ _ article-id-param]] (spy/calls sql/update!)]
          (is (= {:id article-id} article-id-param)))))))

(deftest get-all-articles-test-default-query
  (testing "Test get all articles default database query"
    (with-redefs [sql/query (fn [_ q] q)]
      (let [[query limit] (sut/get-all-articles nil)]
        (is (= (str "SELECT id, user_id, title, featured_image, updated_on, description "
                    "FROM articles ORDER BY updated_on DESC LIMIT ?") query))
        (is (= 100 limit))))))

(deftest get-all-articles-test-query-with-limit
  (testing "Test get all articles with custom limit param"
    (with-redefs [sql/query (fn [_ q] q)]
      (let [limit-param 32
            [_ limit]   (sut/get-all-articles nil limit-param)]
        (is (= limit-param limit))))))

(deftest get-latest-full-sized-articles-query
  (testing "Test get latest full sized articles database query"
    (with-redefs [sql/query (fn [_ q] q)]
      (let [number-param   30
            [query number] (sut/get-latest-full-sized-articles nil number-param)]
        (is (= (str "SELECT id, user_id, title, body, featured_image, created_on, updated_on, description "
                    "FROM articles ORDER BY updated_on DESC LIMIT ?") query))
        (is (= number-param number ))))))

(deftest get-user-articles-test-default-query
  (testing "Test get user articles default database query"
    (with-redefs [sql/query (fn [_ q] q)]
      (let [user-id-param         42
            [query user-id limit] (sut/get-user-articles nil user-id-param)]
        (is (= (str "SELECT id, user_id, title, featured_image, updated_on, description "
                    "FROM articles "
                    "WHERE user_id = ? ORDER BY updated_on DESC LIMIT ?") query))
        (is (= limit 100))
        (is (= user-id user-id-param))))))

(deftest get-user-articles-test-query-with-limit
  (testing "Test get user articles with custom limit param"
    (with-redefs [sql/query (fn [_ q] q)]
      (let [limit-param 32
            [_ _ limit] (sut/get-user-articles nil 1 limit-param)]
        (is (= limit-param limit))))))

(deftest get-article-by-id-test-table-name
  (testing "Test get article by ID database table name"
    (with-redefs [sql/get-by-id (fn [_ t _] t)]
      (is (= :articles (sut/get-article-by-id nil nil))))))

(deftest get-article-by-id-test-article-id-param
  (testing "Test get article by ID article-id param"
    (with-redefs [sql/get-by-id (fn [_ _ i] i)]
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
    (with-redefs [sql/query (fn [_ _] [])]
      (is (= nil (sut/get-last-featured-article nil))))))

(deftest get-last-featured-article-test-first-result
  (testing "Get last featured article should take first result from database"
    (let [res [1 2 3 4 5 6]]
      (with-redefs [sql/query (fn [_ _] res)]
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
    (with-redefs [sql/query (spy/mock (fn [_ _] []))]
      (let [res (sut/get-last-featured-articles nil 3)]
        (is (= [] res))))))

(deftest delete-article-test-table-name
  (testing "Delete article database table name"
    (with-redefs [sql/delete! (fn [_ t _] t)]
      (is (= :articles (sut/delete-article nil nil))))))

(deftest delete-article-test-article-id-param
  (testing "Delete article article-id param"
    (with-redefs [sql/delete! (fn [_ _ i] i)]
      (let [article-id 42]
        (is (= {:id article-id} (sut/delete-article nil article-id)))))))

(deftest can-update-test-success-role
  (doseq [role [:moderator :admin]]
    (testing "Test can-update? returns true for moderator and admin roles"
      (with-redefs [sql/get-by-id (spy/spy)]
        (let [user (->> role name vector (assoc auth-user-deserialized :roles))]
          (is (= true (sut/can-update? nil user nil)))
          (is (spy/not-called? sql/get-by-id)))))))

(deftest can-update-test-article-owner
  (testing "Test can-update? returns true for article owner"
    (let [role       :guest
          user       (->> role name vector (assoc auth-user-deserialized :roles))
          user-id    (:id user)
          article-id 44]
      (with-redefs [sql/get-by-id (spy/mock (fn [_ _ _] {:articles/user_id user-id}))]
        (is (= true (sut/can-update? nil user article-id)))
        (is (spy/called-once-with? sql/get-by-id nil :articles article-id))))))

(deftest can-update-test-guest-not-owner
  (testing "Test can-update? returns false for guest and not article owner"
    (let [role            :guest
          user            (->> role name vector (assoc auth-user-deserialized :roles))
          article-user-id (inc (:id user))]
      (with-redefs [sql/get-by-id (spy/mock (fn [_ _ _] {:articles/user_id article-user-id}))]
        (is (= false (sut/can-update? nil user nil)))
        (is (spy/called-once-with? sql/get-by-id nil :articles nil))))))
