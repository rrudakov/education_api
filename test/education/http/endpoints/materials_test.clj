(ns education.http.endpoints.materials-test
  (:require [cljc.java-time.instant :as instant]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [education.database.materials :as materials-db]
            [education.http.constants :as const]
            [education.http.endpoints.test-app :as test-app]
            [education.test-data :as td]
            [ring.mock.request :as mock]
            [spy.core :as spy]))

(def ^:private test-material-id
  "Test API `material-id`."
  (inc (rand-int 100)))

(def ^:private create-material-request
  "Test API request to create material."
  {:title       "My awesome material"
   :description "This material is for bla-bla-bla"
   :preview     "https://alenkinaskazka.nl/api/materials/bla-bla-bla.png"
   :store_link  "https://some.store/link"
   :price       "99"})

(deftest create-material-test
  (testing "Test POST /materials authorized with admin role and valid request body"
    (with-redefs [materials-db/add-material (spy/stub test-material-id)]
      (let [app      (test-app/api-routes-with-auth)
            response (app (-> (mock/request :post "/api/materials")
                              (mock/header :authorization (td/test-auth-token #{:admin}))
                              (mock/json-body create-material-request)))
            body     (test-app/parse-body (:body response))]
        (is (= 201 (:status response)))
        (is (= {:id test-material-id} body))
        (is (spy/called-once-with? materials-db/add-material nil create-material-request)))))

  (doseq [role [:guest :moderator]]
    (testing (str "Test POST /materials authorized with " role " role and valid request body")
      (with-redefs [materials-db/add-material (spy/spy)]
        (let [app      (test-app/api-routes-with-auth)
              response (app (-> (mock/request :post "/api/materials")
                                (mock/header :authorization (td/test-auth-token #{role}))
                                (mock/json-body create-material-request)))
              body     (test-app/parse-body (:body response))]
          (is (= 403 (:status response)))
          (is (= {:message const/no-access-error-message} body))
          (is (spy/not-called? materials-db/add-material))))))

  (testing "Test POST /materials without authorization header"
    (with-redefs [materials-db/add-material (spy/spy)]
      (let [app      (test-app/api-routes-with-auth)
            response (app (-> (mock/request :post "/api/materials")
                              (mock/json-body create-material-request)))
            body     (test-app/parse-body (:body response))]
        (is (= 401 (:status response)))
        (is (= {:message const/not-authorized-error-message} body))
        (is (spy/not-called? materials-db/add-material)))))

  (doseq [[request-body expected-errors] [[(dissoc create-material-request :title)
                                           ["Field title is mandatory"]]
                                          [(dissoc create-material-request :description)
                                           ["Field description is mandatory"]]
                                          [(dissoc create-material-request :preview)
                                           ["Field preview is mandatory"]]
                                          [(dissoc create-material-request :store_link)
                                           ["Field store_link is mandatory"]]
                                          [(dissoc create-material-request :price)
                                           ["Field price is mandatory"]]
                                          [(assoc create-material-request :title (str/join (repeat 501 "a")))
                                           ["Title must not be longer than 500 characters"]]
                                          [(assoc create-material-request :title 123)
                                           ["Title is not valid"]]
                                          [(assoc create-material-request :description 123)
                                           ["Description is not valid"]]
                                          [(assoc create-material-request :preview "invalid")
                                           ["Preview URL is not valid"]]
                                          [(assoc create-material-request :store_link "invalid")
                                           ["Store_link URL is not valid"]]
                                          [(assoc create-material-request :price "7 euro")
                                           ["Price is not valid"]]
                                          [(assoc create-material-request :price "-123.98")
                                           ["Price is not valid"]]
                                          [(assoc create-material-request :price 34.333)
                                           ["Price is not valid"]]]]
    (testing "Test POST /materials authorized with invalid request body"
      (with-redefs [materials-db/add-material (spy/spy)]
        (let [app      (test-app/api-routes-with-auth)
              response (app (-> (mock/request :post "/api/materials")
                                (mock/header :authorization (td/test-auth-token #{:admin}))
                                (mock/json-body request-body)))
              body     (test-app/parse-body (:body response))]
          (is (= 400 (:status response)))
          (is (= {:message const/bad-request-error-message
                  :errors  expected-errors}
                 (dissoc body :details))))))))

(def ^:private update-material-request
  "Test API request to update material."
  {:title       "Updated material"
   :description "Updated description"
   :preview     "https://alenkinaskazka.nl/api/materials/updated.png"
   :store_link  "https://some.store/updated/link"
   :price       (str (rand 100))})

(deftest update-material-test
  (doseq [request-body (-> (for [k (keys update-material-request)]
                             (dissoc update-material-request k))
                           (conj update-material-request)
                           (conj {}))]
    (testing "Test PATCH /materials/:material-id authorized with admin role and valid request body"
      (with-redefs [materials-db/update-material (spy/stub 1)]
        (let [app      (test-app/api-routes-with-auth)
              response (app (-> (mock/request :patch (str "/api/materials/" test-material-id))
                                (mock/header :authorization (td/test-auth-token #{:admin}))
                                (mock/json-body request-body)))]
          (is (= 204 (:status response)))
          (is (empty? (:body response)))
          (is (spy/called-once-with? materials-db/update-material nil test-material-id request-body))))))

  (doseq [role [:guest :moderator]]
    (testing (str "Test PATCH /materials/:material-id authorized with " role " role and valid request body")
      (with-redefs [materials-db/update-material (spy/spy)]
        (let [app      (test-app/api-routes-with-auth)
              response (app (-> (mock/request :patch (str "/api/materials/" test-material-id))
                                (mock/header :authorization (td/test-auth-token #{role}))
                                (mock/json-body update-material-request)))
              body     (test-app/parse-body (:body response))]
          (is (= 403 (:status response)))
          (is (= {:message const/no-access-error-message} body))
          (is (spy/not-called? materials-db/update-material))))))

  (testing "Test PATCH /materials/:material-id without authorization token"
    (with-redefs [materials-db/update-material (spy/spy)]
      (let [app      (test-app/api-routes-with-auth)
            response (app (-> (mock/request :patch (str "/api/materials/" test-material-id))
                              (mock/json-body update-material-request)))
            body     (test-app/parse-body (:body response))]
        (is (= 401 (:status response)))
        (is (= {:message const/not-authorized-error-message} body))
        (is (spy/not-called? materials-db/update-material)))))

  (testing "Test PATCH /materials/:material-id with invalid `material-id`"
    (with-redefs [materials-db/update-material (spy/spy)]
      (let [app      (test-app/api-routes-with-auth)
            response (app (-> (mock/request :patch "/api/materials/invalid")
                              (mock/header :authorization (td/test-auth-token #{:admin}))
                              (mock/json-body update-material-request)))
            body     (test-app/parse-body (:body response))]
        (is (= 400 (:status response)))
        (is (= {:message const/bad-request-error-message
                :errors  ["Material-id is not valid"]}
               body))
        (is (spy/not-called-with? materials-db/update-material)))))

  (testing "Test PATCH /materials/:material-id with non-existing `material-id`"
    (with-redefs [materials-db/update-material (spy/stub 0)]
      (let [app      (test-app/api-routes-with-auth)
            response (app (-> (mock/request :patch (str "/api/materials/" test-material-id))
                              (mock/header :authorization (td/test-auth-token #{:admin}))
                              (mock/json-body update-material-request)))
            body     (test-app/parse-body (:body response))]
        (is (= 404 (:status response)))
        (is (= {:message const/not-found-error-message} body))
        (is (spy/called-once-with? materials-db/update-material nil test-material-id update-material-request)))))

  (testing "Test PATCH /materials/:material-id unexpected response from database"
    (with-redefs [materials-db/update-material (spy/stub 2)]
      (let [app      (test-app/api-routes-with-auth)
            response (app (-> (mock/request :patch (str "/api/materials/" test-material-id))
                              (mock/header :authorization (td/test-auth-token #{:admin}))
                              (mock/json-body update-material-request)))
            body     (test-app/parse-body (:body response))]
        (is (= 500 (:status response)))
        (is (= {:message const/server-error-message} body))
        (is (spy/called-once-with? materials-db/update-material nil test-material-id update-material-request)))))

  (doseq [[request-body expected-errors] [[(assoc create-material-request :title (str/join (repeat 501 "a")))
                                           ["Title must not be longer than 500 characters"]]
                                          [(assoc create-material-request :title 123)
                                           ["Title is not valid"]]
                                          [(assoc create-material-request :description 123)
                                           ["Description is not valid"]]
                                          [(assoc create-material-request :preview "invalid")
                                           ["Preview URL is not valid"]]
                                          [(assoc create-material-request :store_link "invalid")
                                           ["Store_link URL is not valid"]]
                                          [(assoc create-material-request :price "7 euro")
                                           ["Price is not valid"]]
                                          [(assoc create-material-request :price "-123.98")
                                           ["Price is not valid"]]
                                          [(assoc create-material-request :price 34.333)
                                           ["Price is not valid"]]]]
    (testing "Test PATCH /materials/:material-id with invalid request body"
      (with-redefs [materials-db/update-material (spy/spy)]
        (let [app      (test-app/api-routes-with-auth)
              response (app (-> (mock/request :patch (str "/api/materials/" test-material-id))
                                (mock/header :authorization (td/test-auth-token #{:admin}))
                                (mock/json-body request-body)))
              body     (test-app/parse-body (:body response))]
          (is (= 400 (:status response)))
          (is (= {:message const/bad-request-error-message
                  :errors  expected-errors}
                 body))
          (is (spy/not-called? materials-db/update-material)))))))

(def ^:private material-from-db
  "Test material database query result."
  {:materials/id          (inc (rand-int 100))
   :materials/title       "New material"
   :materials/description "Long material description"
   :materials/preview     "https://alenkinaskazka.nl/img/preview.png"
   :materials/store_link  "https://some.store.com/external/link"
   :materials/price       (bigdec (rand 100))
   :materials/created_on  (instant/now)
   :materials/updated_on  (instant/now)})

(def ^:private material-response-expected
  "Expected API material response."
  {:id          (:materials/id material-from-db)
   :title       (:materials/title material-from-db)
   :description (:materials/description material-from-db)
   :preview     (:materials/preview material-from-db)
   :store_link  (:materials/store_link material-from-db)
   :price       (format "%.2f" (:materials/price material-from-db))
   :created_on  (str (:materials/created_on material-from-db))
   :updated_on  (str (:materials/updated_on material-from-db))})

(deftest get-material-by-id-test
  (testing "Test GET /materials/:material-id with valid `material-id`"
    (with-redefs [materials-db/get-material-by-id (spy/stub material-from-db)]
      (let [app      (test-app/api-routes-with-auth)
            response (app (mock/request :get (str "/api/materials/" test-material-id)))
            body     (test-app/parse-body (:body response))]
        (is (= 200 (:status response)))
        (is (= material-response-expected body))
        (is (spy/called-once-with? materials-db/get-material-by-id nil test-material-id)))))

  (testing "Test GET /materials/:material-id with non-existing `material-id`"
    (with-redefs [materials-db/get-material-by-id (spy/stub nil)]
      (let [app      (test-app/api-routes-with-auth)
            response (app (mock/request :get (str "/api/materials/" test-material-id)))
            body     (test-app/parse-body (:body response))]
        (is (= 404 (:status response)))
        (is (= {:message const/not-found-error-message} body))
        (is (spy/called-once-with? materials-db/get-material-by-id nil test-material-id)))))

  (testing "Test get /materials/:material-id with invalid `material-id`"
    (with-redefs [materials-db/get-material-by-id (spy/spy)]
      (let [app      (test-app/api-routes-with-auth)
            response (app (mock/request :get "/api/materials/invalid"))
            body     (test-app/parse-body (:body response))]
        (is (= 400 (:status response)))
        (is (= {:message const/bad-request-error-message
                :errors  ["Material-id is not valid"]}
               body))
        (is (spy/not-called? materials-db/get-material-by-id))))))

(def ^:private material-from-db-extra
  "One more test material database query result."
  {:materials/id          (inc (rand-int 100))
   :materials/title       "Material 2"
   :materials/description "Material description 2"
   :materials/preview     "https://alenkinaskazka.nl/img/another.png"
   :materials/store_link  "https://some.store.com/another/link"
   :materials/price       (bigdec (rand 100))
   :materials/created_on  (instant/now)
   :materials/updated_on  (instant/now)})

(def ^:private material-response-expected-extra
  "One more expected API material response."
  {:id          (:materials/id material-from-db-extra)
   :title       (:materials/title material-from-db-extra)
   :description (:materials/description material-from-db-extra)
   :preview     (:materials/preview material-from-db-extra)
   :store_link  (:materials/store_link material-from-db-extra)
   :price       (format "%.2f" (:materials/price material-from-db-extra))
   :created_on  (str (:materials/created_on material-from-db-extra))
   :updated_on  (str (:materials/updated_on material-from-db-extra))})

(deftest get-all-materials-test
  (testing "Test GET /materials/ without any query parameters"
    (with-redefs [materials-db/get-all-materials (spy/stub [material-from-db material-from-db-extra])]
      (let [app      (test-app/api-routes-with-auth)
            response (app (mock/request :get "/api/materials"))
            body     (test-app/parse-body (:body response))]
        (is (= 200 (:status response)))
        (is (= [material-response-expected material-response-expected-extra] body))
        (is (spy/called-once-with? materials-db/get-all-materials nil :limit nil :offset nil)))))

  (testing "Test GET /materials with optional `limit` and `offset` parameters"
    (with-redefs [materials-db/get-all-materials (spy/stub [material-from-db material-from-db-extra])]
      (let [offset-param (inc (rand-int 20))
            limit-param  (inc (rand-int 100))
            app          (test-app/api-routes-with-auth)
            response     (app (-> (mock/request :get "/api/materials")
                                  (mock/query-string {:limit  limit-param
                                                      :offset offset-param})))
            body         (test-app/parse-body (:body response))]
        (is (= 200 (:status response)))
        (is (= [material-response-expected material-response-expected-extra] body))
        (is (spy/called-once-with? materials-db/get-all-materials nil :limit limit-param :offset offset-param)))))

  (doseq [[query err] [[{:limit "invalid"} "Limit is not valid"]
                       [{:offset "invalid"} "Offset is not valid"]]]
    (testing "Test GET /materials/ with invalid query parameters"
      (with-redefs [materials-db/get-all-materials (spy/spy)]
        (let [app      (test-app/api-routes-with-auth)
              response (app (-> (mock/request :get "/api/materials")
                                (mock/query-string query)))
              body     (test-app/parse-body (:body response))]
          (is (= 400 (:status response)))
          (is (= {:message const/bad-request-error-message
                  :errors  [err]}
                 body))
          (is (spy/not-called? materials-db/get-all-materials)))))))

(deftest delete-material-by-id-test
  (testing "Test DELETE /materials/:material-id authorized with admin role and valid `material-id`"
    (with-redefs [materials-db/delete-material (spy/stub 1)]
      (let [app      (test-app/api-routes-with-auth)
            response (app (-> (mock/request :delete (str "/api/materials/" test-material-id))
                              (mock/header :authorization (td/test-auth-token #{:admin}))))]
        (is (= 204 (:status response)))
        (is (empty? (:body response)))
        (is (spy/called-once-with? materials-db/delete-material nil test-material-id)))))

  (doseq [role [:quest :moderator]]
    (testing (str "Test DELETE /materials/:material-id authorized with " role " role and valid `material-id`")
      (with-redefs [materials-db/delete-material (spy/spy)]
        (let [app      (test-app/api-routes-with-auth)
              response (app (-> (mock/request :delete (str "/api/materials/" test-material-id))
                                (mock/header :authorization (td/test-auth-token #{role}))))
              body     (test-app/parse-body (:body response))]
          (is (= 403 (:status response)))
          (is (= {:message const/no-access-error-message} body))
          (is (spy/not-called? materials-db/delete-material))))))

  (testing "Test DELETE /materials/:material-id without authorization header"
    (with-redefs [materials-db/delete-material (spy/spy)]
      (let [app      (test-app/api-routes-with-auth)
            response (app (mock/request :delete (str "/api/materials/" test-material-id)))
            body     (test-app/parse-body (:body response))]
        (is (= 401 (:status response)))
        (is (= {:message const/not-authorized-error-message} body))
        (is (spy/not-called? materials-db/delete-material)))))

  (testing "Test DELETE /materials/:material-id with non-existing `material-id`"
    (with-redefs [materials-db/delete-material (spy/stub 0)]
      (let [app      (test-app/api-routes-with-auth)
            response (app (-> (mock/request :delete (str "/api/materials/" test-material-id))
                              (mock/header :authorization (td/test-auth-token #{:admin}))))
            body     (test-app/parse-body (:body response))]
        (is (= 404 (:status response)))
        (is (= {:message const/not-found-error-message} body))
        (is (spy/called-once-with? materials-db/delete-material nil test-material-id)))))

  (testing "Test DELETE /materials/:material-id with invalid `material-id`"
    (with-redefs [materials-db/delete-material (spy/spy)]
      (let [app      (test-app/api-routes-with-auth)
            response (app (-> (mock/request :delete "/api/materials/invalid")
                              (mock/header :authorization (td/test-auth-token #{:admin}))))
            body     (test-app/parse-body (:body response))]
        (is (= 400 (:status response)))
        (is (= {:message const/bad-request-error-message
                :errors  ["Material-id is not valid"]}
               body))
        (is (spy/not-called? materials-db/delete-material)))))

  (testing "Test DELETE /materials/:material-id unexpected result from database"
    (with-redefs [materials-db/delete-material (spy/stub 2)]
      (let [app      (test-app/api-routes-with-auth)
            response (app (-> (mock/request :delete (str "/api/materials/" test-material-id))
                              (mock/header :authorization (td/test-auth-token #{:admin}))))
            body     (test-app/parse-body (:body response))]
        (is (= 500 (:status response)))
        (is (= {:message const/server-error-message} body))
        (is (spy/called-once-with? materials-db/delete-material nil test-material-id))))))
