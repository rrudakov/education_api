(ns education.http.endpoints.lessons-test
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [education.database.lessons :as lessons-db]
            [education.http.constants :as const]
            [education.http.endpoints.lessons :as sut]
            [education.http.endpoints.test-app :as test-app]
            [education.test-data :as td]
            [ring.mock.request :as mock]
            [ring.util.http-response :as status]
            [spy.core :as spy])
  (:import java.sql.SQLException
           java.time.Instant))

(def ^:private test-lesson-id
  "Test API `lesson-id`."
  42)

(def ^:private create-lesson-request
  "Test API request to create lesson."
  {:title       "Lesson title"
   :subtitle    "Lesson subtitle"
   :description "Video lesson description. Very long string"
   :screenshots ["https://alenkinaskazka.net/img/177e97c8-db61-4490-a37b-7edb364ecc8c_Screenshot_20201008-203625.png"
                 "https://alenkinaskazka.net/img/6e8e5e12-8221-4a6b-9ac5-cf4fcaf49b6a_Screenshot_20201008-203702.png"
                 "https://alenkinaskazka.net/img/d4489b05-cb50-4d00-a6c6-bdbb93d0bc92_Screenshot_20201008-203826.png"]
   :price       "7898.40"})

(deftest create-lesson-test
  (doseq [request-body [create-lesson-request
                        (assoc create-lesson-request :screenshots [])
                        (assoc create-lesson-request :price "987987")]]
    (testing "Test POST /lessons authorized with admin role and valid request body"
      (with-redefs [lessons-db/add-lesson (spy/stub test-lesson-id)]
        (let [role     :admin
              app      (test-app/api-routes-with-auth)
              response (app (-> (mock/request :post "/api/lessons")
                                (mock/header :authorization (td/test-auth-token #{role}))
                                (mock/json-body request-body)))
              body     (test-app/parse-body (:body response))]
          (is (= 201 (:status response)))
          (is (= {:id test-lesson-id} body))
          (is (spy/called-once-with? lessons-db/add-lesson nil request-body))))))

  (doseq [role [:guest :moderator]]
    (testing (str "Test POST /lessons authorized with " role " role and valid request body")
      (with-redefs [lessons-db/add-lesson (spy/spy)]
        (let [app      (test-app/api-routes-with-auth)
              response (app (-> (mock/request :post "/api/lessons")
                                (mock/header :authorization (td/test-auth-token #{role}))
                                (mock/json-body create-lesson-request)))
              body     (test-app/parse-body (:body response))]
          (is (= 403 (:status response)))
          (is (spy/not-called? lessons-db/add-lesson))
          (is (= {:message const/no-access-error-message} body))))))

  (testing "Test POST /lessons without authorization header"
    (with-redefs [lessons-db/add-lesson (spy/spy)]
      (let [app      (test-app/api-routes-with-auth)
            response (app (-> (mock/request :post "/api/lessons")
                              (mock/json-body create-lesson-request)))
            body     (test-app/parse-body (:body response))]
        (is (= 401 (:status response)))
        (is (spy/not-called? lessons-db/add-lesson))
        (is (= {:message const/not-authorized-error-message} body)))))

  (doseq [[request-body errors] [[(dissoc create-lesson-request :title)
                                  ["Field title is mandatory"]]
                                 [(dissoc create-lesson-request :subtitle)
                                  ["Field subtitle is mandatory"]]
                                 [(dissoc create-lesson-request :screenshots)
                                  ["Field screenshots is mandatory"]]
                                 [(dissoc create-lesson-request :description)
                                  ["Field description is mandatory"]]
                                 [(dissoc create-lesson-request :price)
                                  ["Field price is mandatory"]]
                                 [(assoc create-lesson-request :title (str/join (repeat 501 "a")))
                                  ["Title must not be longer than 500 characters"]]
                                 [(assoc create-lesson-request :title 123)
                                  ["Title is not valid"]]
                                 [(assoc create-lesson-request :subtitle (str/join (repeat 501 "a")))
                                  ["Subtitle must not be longer than 500 characters"]]
                                 [(assoc create-lesson-request :subtitle 123)
                                  ["Subtitle is not valid"]]
                                 [(assoc create-lesson-request :screenshots "non-list string")
                                  ["Screenshots is not valid"]]
                                 [(assoc create-lesson-request :price "7 euro")
                                  ["Price is not valid"]]
                                 [(assoc create-lesson-request :price "-234.23")
                                  ["Price is not valid"]]]]
    (testing "Test POST /lessons authorized with invalid request body"
      (with-redefs [lessons-db/add-lesson (spy/spy)]
        (let [app      (test-app/api-routes-with-auth)
              response (app (-> (mock/request :post "/api/lessons")
                                (mock/header :authorization (td/test-auth-token #{:admin}))
                                (mock/json-body request-body)))
              body     (test-app/parse-body (:body response))]
          (is (= 400 (:status response)))
          (is (spy/not-called? lessons-db/add-lesson))
          (is (= {:message const/bad-request-error-message
                  :errors  errors}
                 body))))))

  (testing "Test POST /lessons authorized with valid body, database conflict error"
    (with-redefs [lessons-db/add-lesson (spy/mock (fn [_ _] (throw (SQLException. "Conflict" "23505"))))]
      (let [app      (test-app/api-routes-with-auth)
            response (app (-> (mock/request :post "/api/lessons")
                              (mock/header :authorization (td/test-auth-token #{:admin}))
                              (mock/json-body create-lesson-request)))
            body     (test-app/parse-body (:body response))]
        (is (= 409 (:status response)))
        (is (spy/called-once-with? lessons-db/add-lesson nil create-lesson-request))
        (is (= {:message const/conflict-error-message} body)))))

  (testing "Test POST /lessons authorized with valid body, database not found error"
    (with-redefs [lessons-db/add-lesson (spy/mock (fn [_ _] (throw (SQLException. "Not found" "23503"))))]
      (let [app      (test-app/api-routes-with-auth)
            response (app (-> (mock/request :post "/api/lessons")
                              (mock/header :authorization (td/test-auth-token #{:admin}))
                              (mock/json-body create-lesson-request)))
            body     (test-app/parse-body (:body response))]
        (is (= 404 (:status response)))
        (is (spy/called-once-with? lessons-db/add-lesson nil create-lesson-request))
        (is (= {:message const/not-found-error-message} body)))))

  (testing "Test POST /lessons authorized with valid body, database bad query error"
    (with-redefs [lessons-db/add-lesson (spy/mock (fn [_ _] (throw (SQLException. "Bad query" "23502"))))]
      (let [app      (test-app/api-routes-with-auth)
            response (app (-> (mock/request :post "/api/lessons")
                              (mock/header :authorization (td/test-auth-token #{:admin}))
                              (mock/json-body create-lesson-request)))
            body     (test-app/parse-body (:body response))]
        (is (= 400 (:status response)))
        (is (spy/called-once-with? lessons-db/add-lesson nil create-lesson-request))
        (is (= {:message const/bad-request-error-message} body)))))

  (testing "Test POST /lessons authorized with valid body, database unexpected error"
    (with-redefs [lessons-db/add-lesson (spy/mock (fn [_ _] (throw (SQLException. "Shit happens" "987987987"))))]
      (let [app      (test-app/api-routes-with-auth)
            response (app (-> (mock/request :post "/api/lessons")
                              (mock/header :authorization (td/test-auth-token #{:admin}))
                              (mock/json-body create-lesson-request)))
            body     (test-app/parse-body (:body response))]
        (is (= 500 (:status response)))
        (is (spy/called-once-with? lessons-db/add-lesson nil create-lesson-request))
        (is (= {:message "java.sql.SQLException: Shit happens" :error_code "987987987"} body)))))

  (testing "Test POST /lessons authorized with valid body, verify response validation"
    (with-redefs [sut/create-lesson-handler (spy/stub (status/created "/lessons/invalid" {:lessonId "invalid"}))]
      (let [app      (test-app/api-routes-with-auth)
            response (app (-> (mock/request :post "/api/lessons")
                              (mock/header :authorization (td/test-auth-token #{:admin}))
                              (mock/json-body create-lesson-request)))
            body     (test-app/parse-body (:body response))]
        (is (= 500 (:status response)))
        (is (= {:message const/server-error-message} (dissoc body :details)))))))

(def ^:private update-lesson-request
  "Test API request to update lesson."
  {:title       "Lesson title"
   :subtitle    "Lesson subtitle"
   :description "Video lesson description. Very long string"
   :screenshots ["https://alenkinaskazka.net/img/177e97c8-db61-4490-a37b-7edb364ecc8c_Screenshot_20201008-203625.png"
                 "https://alenkinaskazka.net/img/6e8e5e12-8221-4a6b-9ac5-cf4fcaf49b6a_Screenshot_20201008-203702.png"
                 "https://alenkinaskazka.net/img/d4489b05-cb50-4d00-a6c6-bdbb93d0bc92_Screenshot_20201008-203826.png"]
   :price       "7898.40"})

(deftest update-lesson-test
  (doseq [request-body [update-lesson-request
                        (dissoc update-lesson-request :title)
                        (dissoc update-lesson-request :subtitle)
                        (dissoc update-lesson-request :description)
                        (dissoc update-lesson-request :screenshots)
                        (dissoc update-lesson-request :price)]]
    (testing "Test PATCH /lessons/:lesson-id authorized with admin role and valid request body"
      (with-redefs [lessons-db/update-lesson (spy/stub 1)]
        (let [role     :admin
              app      (test-app/api-routes-with-auth)
              response (app (-> (mock/request :patch (str "/api/lessons/" test-lesson-id))
                                (mock/header :authorization (td/test-auth-token #{role}))
                                (mock/json-body request-body)))]
          (is (= 204 (:status response)))
          (is (spy/called-once-with? lessons-db/update-lesson nil test-lesson-id request-body))
          (is (empty? (:body response)))))))

  (doseq [role [:guest :moderator]]
    (testing (str "Test PATCH /lessons/:lesson-id authorized with " role " role and valid request body")
      (with-redefs [lessons-db/update-lesson (spy/spy)]
        (let [app      (test-app/api-routes-with-auth)
              response (app (-> (mock/request :patch (str "/api/lessons/" test-lesson-id))
                                (mock/header :authorization (td/test-auth-token #{role}))
                                (mock/json-body update-lesson-request)))
              body     (test-app/parse-body (:body response))]
          (is (= 403 (:status response)))
          (is (spy/not-called? lessons-db/update-lesson))
          (is (= {:message const/no-access-error-message} body))))))

  (testing "Test PATCH /lessons/:lesson-id without authorization header"
    (with-redefs [lessons-db/update-lesson (spy/spy)]
      (let [app      (test-app/api-routes-with-auth)
            response (app (-> (mock/request :patch (str "/api/lessons/" test-lesson-id))
                              (mock/json-body update-lesson-request)))
            body     (test-app/parse-body (:body response))]
        (is (= 401 (:status response)))
        (is (spy/not-called? lessons-db/update-lesson))
        (is (= {:message const/not-authorized-error-message} body)))))

  (testing "Test PATCH /lessons/:lesson-id with invalid `lesson-id`"
    (with-redefs [lessons-db/update-lesson (spy/spy)]
      (let [app      (test-app/api-routes-with-auth)
            response (app (-> (mock/request :patch "/api/lessons/invalid")
                              (mock/header :authorization (td/test-auth-token #{:admin}))
                              (mock/json-body update-lesson-request)))
            body     (test-app/parse-body (:body response))]
        (is (= 400 (:status response)))
        (is (spy/not-called? lessons-db/update-lesson))
        (is (= {:message const/bad-request-error-message
                :errors  ["Field lesson-id is mandatory"]}
               body)))))

  (testing "Test PATCH /lessons/:lesson-id with non-existing `lesson-id`"
    (with-redefs [lessons-db/update-lesson (spy/stub 0)]
      (let [app      (test-app/api-routes-with-auth)
            response (app (-> (mock/request :patch (str "/api/lessons/" test-lesson-id))
                              (mock/header :authorization (td/test-auth-token #{:admin}))
                              (mock/json-body update-lesson-request)))
            body     (test-app/parse-body (:body response))]
        (is (= 404 (:status response)))
        (is (spy/called-once-with? lessons-db/update-lesson nil test-lesson-id update-lesson-request))
        (is (= {:message const/not-found-error-message} body)))))

  (testing "Test PATCH /lessons/:lesson-id unexpected response from database"
    (with-redefs [lessons-db/update-lesson (spy/stub 2)]
      (let [app      (test-app/api-routes-with-auth)
            response (app (-> (mock/request :patch (str "/api/lessons/" test-lesson-id))
                              (mock/header :authorization (td/test-auth-token #{:admin}))
                              (mock/json-body update-lesson-request)))
            body     (test-app/parse-body (:body response))]
        (is (= 500 (:status response)))
        (is (spy/called-once-with? lessons-db/update-lesson nil test-lesson-id update-lesson-request))
        (is (= {:message const/server-error-message} body)))))

  (doseq [[request-body errors] [[(assoc create-lesson-request :title (str/join (repeat 501 "a")))
                                  ["Title must not be longer than 500 characters"]]
                                 [(assoc create-lesson-request :title 123)
                                  ["Title is not valid"]]
                                 [(assoc create-lesson-request :subtitle (str/join (repeat 501 "a")))
                                  ["Subtitle must not be longer than 500 characters"]]
                                 [(assoc create-lesson-request :subtitle 123)
                                  ["Subtitle is not valid"]]
                                 [(assoc create-lesson-request :screenshots "non-list string")
                                  ["Screenshots is not valid"]]
                                 [(assoc create-lesson-request :price "7 euro")
                                  ["Price is not valid"]]
                                 [(assoc create-lesson-request :price "-234.23")
                                  ["Price is not valid"]]]]
    (testing "Test PATCH /lessons/:lesson-id with invalid request body"
      (with-redefs [lessons-db/update-lesson (spy/spy)]
        (let [app      (test-app/api-routes-with-auth)
              response (app (-> (mock/request :patch (str "/api/lessons/" test-lesson-id))
                                (mock/header :authorization (td/test-auth-token #{:admin}))
                                (mock/json-body request-body)))
              body     (test-app/parse-body (:body response))]
          (is (= 400 (:status response)))
          (is (spy/not-called? lessons-db/update-lesson))
          (is (= {:message const/bad-request-error-message
                  :errors  errors}
                 body)))))))

(def ^:private lesson-from-db
  "Test lesson database query result."
  {:lessons/id          19
   :lessons/title       "Video lesson"
   :lessons/subtitle    "Video subtitle"
   :lessons/description "Long video lesson description"
   :lessons/screenshots ["http://first.screenshot.com" "http://second.screenshot.com"]
   :lessons/price       (bigdec "22.50000")
   :lessons/created_on  (Instant/parse "2020-05-27T18:28:12Z")
   :lessons/updated_on  (Instant/parse "2020-05-27T18:28:12Z")})

(def ^:private lesson-response-expected
  "Expected API lesson response."
  {:id          (:lessons/id lesson-from-db)
   :title       (:lessons/title lesson-from-db)
   :subtitle    (:lessons/subtitle lesson-from-db)
   :description (:lessons/description lesson-from-db)
   :screenshots (:lessons/screenshots lesson-from-db)
   :price       (format "%.2f" (:lessons/price lesson-from-db))
   :created_on  (str (:lessons/created_on lesson-from-db))
   :updated_on  (str (:lessons/updated_on lesson-from-db))})

(deftest get-lesson-by-id-test
  (testing "Test GET /lessons/:lesson-id with valid `lesson-id`"
    (with-redefs [lessons-db/get-lesson-by-id (spy/stub lesson-from-db)]
      (let [app      (test-app/api-routes-with-auth)
            response (app (mock/request :get (str "/api/lessons/" test-lesson-id)))
            body     (test-app/parse-body (:body response))]
        (is (= 200 (:status response)))
        (is (= lesson-response-expected body))
        (is (spy/called-once-with? lessons-db/get-lesson-by-id nil test-lesson-id)))))

  (testing "Test GET /lessons/:lesson-id with non-existing `lesson-id`"
    (with-redefs [lessons-db/get-lesson-by-id (spy/stub nil)]
      (let [app      (test-app/api-routes-with-auth)
            response (app (mock/request :get (str "/api/lessons/" test-lesson-id)))
            body     (test-app/parse-body (:body response))]
        (is (= 404 (:status response)))
        (is (= {:message const/not-found-error-message} body))
        (is (spy/called-once-with? lessons-db/get-lesson-by-id nil test-lesson-id)))))

  (testing "Test GET /lessons/:lesson-id with invalid `lesson-id`"
    (with-redefs [lessons-db/get-lesson-by-id (spy/spy)]
      (let [app      (test-app/api-routes-with-auth)
            response (app (mock/request :get "/api/lessons/invalid"))
            body     (test-app/parse-body (:body response))]
        (is (= 400 (:status response)))
        (is (= {:message const/bad-request-error-message
                :errors  ["Value is not valid"]}
               body))
        (is (spy/not-called? lessons-db/get-lesson-by-id))))))

(def ^:private lesson-from-db-extra
  "One more test lesson database query result."
  {:lessons/id          20
   :lessons/title       "Video lesson 2"
   :lessons/subtitle    "Video subtitle"
   :lessons/description "Long video lesson description"
   :lessons/screenshots ["http://first.screenshot.com" "http://second.screenshot.com"]
   :lessons/price       (bigdec "7.0000")
   :lessons/created_on  (Instant/parse "2020-05-28T18:28:12Z")
   :lessons/updated_on  (Instant/parse "2020-05-28T18:28:12Z")})

(def ^:private lesson-response-expected-extra
  "One more expected API lesson response."
  {:id          (:lessons/id lesson-from-db-extra)
   :title       (:lessons/title lesson-from-db-extra)
   :subtitle    (:lessons/subtitle lesson-from-db-extra)
   :description (:lessons/description lesson-from-db-extra)
   :screenshots (:lessons/screenshots lesson-from-db-extra)
   :price       (format "%.2f" (:lessons/price lesson-from-db-extra))
   :created_on  (str (:lessons/created_on lesson-from-db-extra))
   :updated_on  (str (:lessons/updated_on lesson-from-db-extra))})

(deftest get-all-lessons-test
  (testing "Test GET /lessons/ without any query parameters"
    (with-redefs [lessons-db/get-all-lessons (spy/stub [lesson-from-db lesson-from-db-extra])]
      (let [app      (test-app/api-routes-with-auth)
            response (app (mock/request :get "/api/lessons"))
            body     (test-app/parse-body (:body response))]
        (is (= 200 (:status response)))
        (is (= [lesson-response-expected lesson-response-expected-extra] body))
        (is (spy/called-once-with? lessons-db/get-all-lessons nil :limit nil :offset nil)))))

  (testing "Test GET /lessons/ with optional `limit` and `offset` parameters"
    (with-redefs [lessons-db/get-all-lessons (spy/stub [lesson-from-db lesson-from-db-extra])]
      (let [app          (test-app/api-routes-with-auth)
            limit-param  99
            offset-param 20
            response     (app (-> (mock/request :get "/api/lessons")
                                  (mock/query-string {:limit  limit-param
                                                      :offset offset-param})))
            body         (test-app/parse-body (:body response))]
        (is (= 200 (:status response)))
        (is (= [lesson-response-expected lesson-response-expected-extra] body))
        (is (spy/called-once-with? lessons-db/get-all-lessons nil :limit limit-param :offset offset-param)))))

  (doseq [query [{:limit "invalid"}
                 {:offset "invalid"}]]
    (testing "Test GET /lessons/ with invalid query parameters"
      (with-redefs [lessons-db/get-all-lessons (spy/spy)]
        (let [app      (test-app/api-routes-with-auth)
              response (app (-> (mock/request :get "/api/lessons")
                                (mock/query-string query)))
              body     (test-app/parse-body (:body response))]
          (is (= 400 (:status response)))
          (is (= {:message const/bad-request-error-message
                  :errors  ["Value is not valid"]}
                 body))
          (is (spy/not-called? lessons-db/get-all-lessons)))))))

(deftest delete-lesson-by-id-test
  (testing "Test DELETE /lessons/:lesson-id authorized with :admin role and valid `lesson-id`"
    (with-redefs [lessons-db/delete-lesson (spy/stub 1)]
      (let [app      (test-app/api-routes-with-auth)
            response (app (-> (mock/request :delete (str "/api/lessons/" test-lesson-id))
                              (mock/header :authorization (td/test-auth-token #{:admin}))))]
        (is (= 204 (:status response)))
        (is (empty? (:body response)))
        (is (spy/called-once-with? lessons-db/delete-lesson nil test-lesson-id)))))

  (doseq [role [:moderator :guest]]
    (testing (str "Test DELETE /lessons/:lesson-id authorized with " role " role and valid `lesson-id`")
      (with-redefs [lessons-db/delete-lesson (spy/spy)]
        (let [app      (test-app/api-routes-with-auth)
              response (app (-> (mock/request :delete (str "/api/lessons/" test-lesson-id))
                                (mock/header :authorization (td/test-auth-token #{role}))))
              body     (test-app/parse-body (:body response))]
          (is (= 403 (:status response)))
          (is (= {:message const/no-access-error-message} body))
          (is (spy/not-called? lessons-db/delete-lesson))))))

  (testing "Test DELETE /lessons/:lesson-id without authorization header"
    (with-redefs [lessons-db/delete-lesson (spy/spy)]
      (let [app      (test-app/api-routes-with-auth)
            response (app (mock/request :delete (str "/api/lessons/" test-lesson-id)))
            body     (test-app/parse-body (:body response))]
        (is (= 401 (:status response)))
        (is (= {:message const/not-authorized-error-message} body))
        (is (spy/not-called? lessons-db/delete-lesson)))))

  (testing "Test DELETE /lessons/:lesson-id for non-existing `lesson-id`"
    (with-redefs [lessons-db/delete-lesson (spy/stub 0)]
      (let [app      (test-app/api-routes-with-auth)
            response (app (-> (mock/request :delete (str "/api/lessons/" test-lesson-id))
                              (mock/header :authorization (td/test-auth-token #{:admin}))))
            body     (test-app/parse-body (:body response))]
        (is (= 404 (:status response)))
        (is (= {:message const/not-found-error-message} body))
        (is (spy/called-once-with? lessons-db/delete-lesson nil test-lesson-id)))))

  (testing "Test DELETE /lessons/:lesson-id with invalid `lesson-id`"
    (with-redefs [lessons-db/delete-lesson (spy/spy)]
      (let [app      (test-app/api-routes-with-auth)
            response (app (-> (mock/request :delete "/api/lessons/invalid")
                              (mock/header :authorization (td/test-auth-token #{:admin}))))
            body     (test-app/parse-body (:body response))]
        (is (= 400 (:status response)))
        (is (= {:message const/bad-request-error-message
                :errors  ["Value is not valid"]}
               body))
        (is (spy/not-called? lessons-db/delete-lesson)))))

  (testing "Test DELETE /lessons/:lesson-id unexpected result from database"
    (with-redefs [lessons-db/delete-lesson (spy/stub 2)]
      (let [app      (test-app/api-routes-with-auth)
            response (app (-> (mock/request :delete (str "/api/lessons/" test-lesson-id))
                              (mock/header :authorization (td/test-auth-token #{:admin}))))
            body     (test-app/parse-body (:body response))]
        (is (= 500 (:status response)))
        (is (= {:message const/server-error-message} body))
        (is (spy/called-once-with? lessons-db/delete-lesson nil test-lesson-id))))))
