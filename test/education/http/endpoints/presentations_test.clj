(ns education.http.endpoints.presentations-test
  (:require [cheshire.core :as cheshire]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [education.database.presentations :as presentations-db]
            [education.http.constants :as const]
            [education.http.endpoints.test-app :as test-app]
            [education.test-data :as td]
            [ring.mock.request :as mock]
            [spy.core :as spy])
  (:import java.time.Instant))

(def ^:private test-presentation-id
  "Test API `presentation-id`."
  83)

(def ^:private create-presentation-request
  "Test API request to create new presentation."
  {:title       "New presentation"
   :url         "https://google.com/api/presentation"
   :description "This presentation is about bla-bla-bla..."})

(def ^:private create-presentation-request-v2
  "Test API request to create new presentation v2."
  {:title       "New presentation"
   :url         "https://google.com/api/presentation"
   :description "This presentation is about bla-bla-bla..."
   :is_public   false
   :attachment  "https://alenkinaskazka.net/some_file.pdf"
   :preview     "https://alenkinaskazka.net/some_image.png"})

(deftest create-presentation-test
  (testing "Test POST /presentations authorized with admin role and valid request body"
    (with-redefs [presentations-db/add-presentation (spy/stub test-presentation-id)]
      (let [role     :admin
            app      (test-app/api-routes-with-auth)
            response (app (-> (mock/request :post "/api/presentations")
                              (mock/content-type td/application-json)
                              (mock/header :authorization (td/test-auth-token #{role}))
                              (mock/body (cheshire/generate-string create-presentation-request))))
            body     (test-app/parse-body (:body response))]
        (is (= 201 (:status response)))
        (is (= {:id test-presentation-id} body))
        (is (spy/called-once-with? presentations-db/add-presentation nil create-presentation-request)))))

  (testing "Test POST /presentations authorized with admin role and valid request body v2"
    (with-redefs [presentations-db/add-presentation (spy/stub test-presentation-id)]
      (let [app      (test-app/api-routes-with-auth)
            response (app (-> (mock/request :post "/api/presentations")
                              (mock/content-type td/application-json)
                              (mock/header :authorization (td/test-auth-token #{:admin}))
                              (mock/body (cheshire/generate-string create-presentation-request-v2))))
            body     (test-app/parse-body (:body response))]
        (is (= 201 (:status response)))
        (is (= {:id test-presentation-id} body))
        (is (spy/called-once-with? presentations-db/add-presentation nil create-presentation-request-v2)))))

  (doseq [role [:guest :moderator]]
    (testing (str "Test POST /presentations authorized with " role " role and valid request body")
      (with-redefs [presentations-db/add-presentation (spy/spy)]
        (let [app      (test-app/api-routes-with-auth)
              response (app (-> (mock/request :post "/api/presentations")
                                (mock/content-type td/application-json)
                                (mock/header :authorization (td/test-auth-token #{role}))
                                (mock/body (cheshire/generate-string create-presentation-request))))
              body     (test-app/parse-body (:body response))]
          (is (= 403 (:status response)))
          (is (spy/not-called? presentations-db/add-presentation))
          (is (= {:message const/no-access-error-message} body))))))

  (testing "Test POST /presentations without authorization header"
    (with-redefs [presentations-db/add-presentation (spy/spy)]
      (let [app      (test-app/api-routes-with-auth)
            response (app (-> (mock/request :post "/api/presentations")
                              (mock/content-type td/application-json)
                              (mock/body (cheshire/generate-string create-presentation-request))))
            body     (test-app/parse-body (:body response))]
        (is (= 401 (:status response)))
        (is (spy/not-called? presentations-db/add-presentation))
        (is (= {:message const/not-authorized-error-message} body)))))

  (doseq [[request-body errors] [[(dissoc create-presentation-request :title)
                                  ["Field title is mandatory"]]
                                 [(dissoc create-presentation-request :url)
                                  ["Field url is mandatory"]]
                                 [(dissoc create-presentation-request :description)
                                  ["Field description is mandatory"]]
                                 [(assoc create-presentation-request :title (str/join (repeat 501 "a")))
                                  ["Title must not be longer than 500 characters"]]
                                 [(assoc create-presentation-request :title 123)
                                  ["Title is not valid"]]
                                 [(assoc create-presentation-request :url "invalid")
                                  ["Url URL is not valid"]]
                                 [(assoc create-presentation-request :url 2828)
                                  ["Url is not valid"]]
                                 [(assoc create-presentation-request :description 1234)
                                  ["Description is not valid"]]
                                 [(assoc create-presentation-request-v2 :is_public "invalid")
                                  ["Is_public is not valid"]]
                                 [(assoc create-presentation-request-v2 :attachment nil)
                                  ["Attachment is not valid"]]
                                 [(assoc create-presentation-request-v2 :attachment "invalid")
                                  ["Attachment URL is not valid"]]
                                 [(assoc create-presentation-request-v2 :preview "invalid")
                                  ["Preview URL is not valid"]]]]
    (testing "Test POST /presentations authorized with invalid request body"
      (let [app      (test-app/api-routes-with-auth)
            response (app (-> (mock/request :post "/api/presentations")
                              (mock/content-type td/application-json)
                              (mock/header :authorization (td/test-auth-token #{:admin}))
                              (mock/body (cheshire/generate-string request-body))))
            body     (test-app/parse-body (:body response))]
        (is (= 400 (:status response)))
        (is (spy/not-called? presentations-db/add-presentation))
        (is (= {:message const/bad-request-error-message
                :errors  errors}
               body))))))

(def ^:private update-presentation-request
  "Test API request to update presentation."
  {:title       "Presentation title"
   :url         "https://google.com/api/presentation/some/fancy/stuff/"
   :description "Updated description for presentation"})

(def ^:private update-presentation-request-v2
  "Test API request to update presentation v2."
  {:title       "Presentation title"
   :url         "https://google.com/api/presentation/some/fancy/stuff/"
   :description "Updated description for presentation"
   :is_public   false
   :attachment  "https://alenkinaskazka.net/some-file.pdf"
   :preview     "https://alenkinaskazka.net/some_image.png"})

(deftest update-presentation-test
  (doseq [request-body [update-presentation-request
                        (dissoc update-presentation-request-v2 :title)
                        (dissoc update-presentation-request-v2 :url)
                        (dissoc update-presentation-request-v2 :description)
                        (dissoc update-presentation-request-v2 :is_public)
                        (dissoc update-presentation-request-v2 :attachment)
                        (dissoc update-presentation-request-v2 :preview)
                        {}]]
    (testing "Test PATCH /presentations/:presentation-id authorized with admin role and valid request body"
      (with-redefs [presentations-db/update-presentation (spy/stub 1)]
        (let [app      (test-app/api-routes-with-auth)
              response (app (-> (mock/request :patch (str "/api/presentations/" test-presentation-id))
                                (mock/content-type td/application-json)
                                (mock/header :authorization (td/test-auth-token #{:admin}))
                                (mock/body (cheshire/generate-string request-body))))]
          (is (= 204 (:status response)))
          (is (spy/called-once-with? presentations-db/update-presentation nil test-presentation-id request-body))
          (is (empty? (:body response)))))))

  (doseq [role [:guest :moderator]]
    (testing (str "Test PATCH /presentations/:presentation-id authorized with " role " role and valid request body")
      (with-redefs [presentations-db/update-presentation (spy/spy)]
        (let [app      (test-app/api-routes-with-auth)
              response (app (-> (mock/request :patch (str "/api/presentations/" test-presentation-id))
                                (mock/content-type td/application-json)
                                (mock/header :authorization (td/test-auth-token #{role}))
                                (mock/body (cheshire/generate-string update-presentation-request))))
              body     (test-app/parse-body (:body response))]
          (is (= 403 (:status response)))
          (is (spy/not-called? presentations-db/update-presentation))
          (is (= {:message const/no-access-error-message} body))))))

  (testing "Test PATCH /presentations/:presentation-id without authorization token"
    (with-redefs [presentations-db/update-presentation (spy/spy)]
      (let [app      (test-app/api-routes-with-auth)
            response (app (-> (mock/request :patch (str "/api/presentations/" test-presentation-id))
                              (mock/content-type td/application-json)
                              (mock/body (cheshire/generate-string update-presentation-request))))
            body     (test-app/parse-body (:body response))]
        (is (= 401 (:status response)))
        (is (spy/not-called? presentations-db/update-presentation))
        (is (= {:message const/not-authorized-error-message} body)))))

  (testing "Test PATCH /presentations/:presentation-id with invalid `presentation-id`"
    (with-redefs [presentations-db/update-presentation (spy/spy)]
      (let [app      (test-app/api-routes-with-auth)
            response (app (-> (mock/request :patch "/api/presentations/invalid")
                              (mock/content-type td/application-json)
                              (mock/header :authorization (td/test-auth-token #{:admin}))
                              (mock/body (cheshire/generate-string update-presentation-request))))
            body     (test-app/parse-body (:body response))]
        (is (= 400 (:status response)))
        (is (spy/not-called? presentations-db/update-presentation))
        (is (= {:message const/bad-request-error-message
                :errors  ["Field presentation-id is mandatory"]}
               body)))))

  (testing "Test PATCH /presentations/:presentation-id with non-existing `presentation-id`"
    (with-redefs [presentations-db/update-presentation (spy/stub 0)]
      (let [app      (test-app/api-routes-with-auth)
            response (app (-> (mock/request :patch (str "/api/presentations/" test-presentation-id))
                              (mock/content-type td/application-json)
                              (mock/header :authorization (td/test-auth-token #{:admin}))
                              (mock/body (cheshire/generate-string update-presentation-request))))
            body     (test-app/parse-body (:body response))]
        (is (= 404 (:status response)))
        (is (spy/called-once-with? presentations-db/update-presentation nil test-presentation-id update-presentation-request))
        (is (= {:message const/not-found-error-message} body)))))

  (testing "Test PATCH /presentations/:presentation-id unexpected response from database"
    (with-redefs [presentations-db/update-presentation (spy/stub 2)]
      (let [app      (test-app/api-routes-with-auth)
            response (app (-> (mock/request :patch (str "/api/presentations/" test-presentation-id))
                              (mock/content-type td/application-json)
                              (mock/header :authorization (td/test-auth-token #{:admin}))
                              (mock/body (cheshire/generate-string update-presentation-request))))
            body     (test-app/parse-body (:body response))]
        (is (= 500 (:status response)))
        (is (spy/called-once-with? presentations-db/update-presentation nil test-presentation-id update-presentation-request))
        (is (= {:message const/server-error-message} body)))))

  (doseq [[request-body errors] [[(assoc update-presentation-request :title (str/join (repeat 501 "a")))
                                  ["Title must not be longer than 500 characters"]]
                                 [(assoc update-presentation-request :title 123)
                                  ["Title is not valid"]]
                                 [(assoc update-presentation-request :url "invalid")
                                  ["Url URL is not valid"]]
                                 [(assoc update-presentation-request :url 123)
                                  ["Url is not valid"]]
                                 [(assoc update-presentation-request :description 1234)
                                  ["Description is not valid"]]
                                 [(assoc update-presentation-request :is_public "invalid")
                                  ["Is_public is not valid"]]
                                 [(assoc update-presentation-request-v2 :attachment nil)
                                  ["Attachment is not valid"]]
                                 [(assoc update-presentation-request-v2 :attachment "invalid")
                                  ["Attachment URL is not valid"]]
                                 [(assoc update-presentation-request-v2 :preview "invalid")
                                  ["Preview URL is not valid"]]]]
    (testing "Test PATCH /presentations/:presentation-id with invalid request body"
      (with-redefs [presentations-db/update-presentation (spy/spy)]
        (let [app      (test-app/api-routes-with-auth)
              response (app (-> (mock/request :patch (str "/api/presentations/" test-presentation-id))
                                (mock/content-type td/application-json)
                                (mock/header :authorization (td/test-auth-token #{:admin}))
                                (mock/body (cheshire/generate-string request-body))))
              body     (test-app/parse-body (:body response))]
          (is (= 400 (:status response)))
          (is (spy/not-called? presentations-db/update-presentation))
          (is (= {:message const/bad-request-error-message
                  :errors  errors}
                 body)))))))

(def ^:private presentation-from-db
  "Test presentation database query result."
  {:presentations/id          23
   :presentations/title       "The presentation"
   :presentations/url         "https://google.com/presentations/api/bla"
   :presentations/description "This is presentation about bla-bla.."
   :presentations/is_public   true
   :presentations/attachment  "https://alenkinaskazka.net/some_file.pdf"
   :presentations/preview     "https://alenkinaskazka.net/some_image.png"
   :presentations/created_on  (Instant/now)
   :presentations/updated_on  (Instant/now)})

(def ^:private presentation-response-expected
  "Expected API presentation response."
  {:id          (:presentations/id presentation-from-db)
   :title       (:presentations/title presentation-from-db)
   :url         (:presentations/url presentation-from-db)
   :description (:presentations/description presentation-from-db)
   :is_public   (:presentations/is_public presentation-from-db)
   :attachment  (:presentations/attachment presentation-from-db)
   :preview     (:presentations/preview presentation-from-db)
   :created_on  (str (:presentations/created_on presentation-from-db))
   :updated_on  (str (:presentations/updated_on presentation-from-db))})

(deftest get-presentation-by-id-test
  (testing "Test GET /presentations/:presentation-id authorized with admin role and valid `presentation-id`"
    (with-redefs [presentations-db/get-presentation-by-id (spy/stub presentation-from-db)]
      (let [app      (test-app/api-routes-with-auth)
            response (app (-> (mock/request :get (str "/api/presentations/" test-presentation-id))
                              (mock/header :authorization (td/test-auth-token #{:admin}))))
            body     (test-app/parse-body (:body response))]
        (is (= 200 (:status response)))
        (is (= presentation-response-expected body))
        (is (spy/called-once-with? presentations-db/get-presentation-by-id nil test-presentation-id)))))

  (testing "Test GET /presentations/:presentation-id authorized with admin role and valid `presentation-id` no attachment"
    (let [presentation-db   (assoc presentation-from-db :attachment nil)
          response-expected (dissoc presentation-response-expected :attachment)]
      (with-redefs [presentations-db/get-presentation-by-id (spy/stub presentation-db)]
        (let [app      (test-app/api-routes-with-auth)
              response (app (-> (mock/request :get (str "/api/presentations/" test-presentation-id))
                                (mock/header :authorization (td/test-auth-token #{:admin}))))
              body     (test-app/parse-body (:body response))]
          (is (= 200 (:status response)))
          (is (= response-expected body))
          (is (spy/called-once-with? presentations-db/get-presentation-by-id nil test-presentation-id))))))

  (doseq [role [:moderator :guest]]
    (testing (str "Test GET /presentations/:presentation-id authorizad with " role " role and valid `presentation-id`")
      (with-redefs [presentations-db/get-presentation-by-id (spy/spy)]
        (let [app      (test-app/api-routes-with-auth)
              response (app (-> (mock/request :get (str "/api/presentations/" test-presentation-id))
                                (mock/header :authorization (td/test-auth-token #{role}))))
              body     (test-app/parse-body (:body response))]
          (is (= 403 (:status response)))
          (is (= {:message const/no-access-error-message} body))
          (is (spy/not-called? presentations-db/get-presentation-by-id))))))

  (testing "Test GET /presentations/:presentation-id without authorization token"
    (with-redefs [presentations-db/get-presentation-by-id (spy/spy)]
      (let [app      (test-app/api-routes-with-auth)
            response (app (mock/request :get (str "/api/presentations/" test-presentation-id)))
            body     (test-app/parse-body (:body response))]
        (is (= 401 (:status response)))
        (is (= {:message const/not-authorized-error-message} body))
        (is (spy/not-called? presentations-db/get-presentation-by-id)))))

  (testing "Test GET /presentations/:presentstion-id authorized with admin role and non-existing `presentation-id`"
    (with-redefs [presentations-db/get-presentation-by-id (spy/spy)]
      (let [app      (test-app/api-routes-with-auth)
            response (app (-> (mock/request :get (str "/api/presentations/" test-presentation-id))
                              (mock/header :authorization (td/test-auth-token #{:admin}))))
            body     (test-app/parse-body (:body response))]
        (is (= 404 (:status response)))
        (is (= {:message const/not-found-error-message} body))
        (is (spy/called-once-with? presentations-db/get-presentation-by-id nil test-presentation-id)))))

  (testing "Test get /presentations/:presentation-id authorized with admin role and invalid `presentation-id`"
    (with-redefs [presentations-db/get-presentation-by-id (spy/spy)]
      (let [app      (test-app/api-routes-with-auth)
            response (app (-> (mock/request :get "/api/presentations/invalid")
                              (mock/header :authorization (td/test-auth-token #{:admin}))))
            body     (test-app/parse-body (:body response))]
        (is (= 400 (:status response)))
        (is (spy/not-called? presentations-db/get-presentation-by-id))
        (is (= {:message const/bad-request-error-message
                :errors  ["Value is not valid"]}
               body))))))

(def ^:private presentation-from-db-extra
  "One more test presentation database query result."
  {:presentations/id          22
   :presentations/title       "The presentation 2"
   :presentations/description "This is another presentation about bla-bla.."
   :presentations/is_public   false
   :presentations/attachment  nil
   :presentations/preview     nil
   :presentations/created_on  (Instant/now)
   :presentations/updated_on  (Instant/now)})

(def ^:private presentation-response-expected-extra
  "One more expected API presentation response."
  {:id          (:presentations/id presentation-from-db-extra)
   :title       (:presentations/title presentation-from-db-extra)
   :description (:presentations/description presentation-from-db-extra)
   :is_public   (:presentations/is_public presentation-from-db-extra)
   :created_on  (str (:presentations/created_on presentation-from-db-extra))
   :updated_on  (str (:presentations/updated_on presentation-from-db-extra))})

(deftest get-all-presentations-test
  (testing "Test GET /presentations without any query parameters"
    (with-redefs [presentations-db/get-all-presentations (spy/stub [presentation-from-db presentation-from-db-extra])]
      (let [app      (test-app/api-routes-with-auth)
            response (app (mock/request :get "/api/presentations"))
            body     (test-app/parse-body (:body response))]
        (is (= 200 (:status response)))
        (is (= [presentation-response-expected presentation-response-expected-extra] body))
        (is (spy/called-once-with? presentations-db/get-all-presentations nil :limit nil :offset nil)))))

  (testing "Test GET /presentations with optional `limit` and `offset` parameters"
    (with-redefs [presentations-db/get-all-presentations (spy/stub [presentation-from-db presentation-from-db-extra])]
      (let [app          (test-app/api-routes-with-auth)
            limit-param  838
            offset-param 99
            response     (app (mock/request :get (str "/api/presentations?limit=" limit-param "&offset=" offset-param)))
            body         (test-app/parse-body (:body response))]
        (is (= 200 (:status response)))
        (is (= [presentation-response-expected presentation-response-expected-extra] body))
        (is (spy/called-once-with? presentations-db/get-all-presentations nil :limit limit-param :offset offset-param)))))

  (doseq [url ["/api/presentations?limit=invalid"
               "/api/presentations?offset=invalid"]]
    (testing "Test GET /presentations with invalid query parameters"
      (with-redefs [presentations-db/get-all-presentations (spy/spy)]
        (let [app      (test-app/api-routes-with-auth)
              response (app (mock/request :get url))
              body     (test-app/parse-body (:body response))]
          (is (= 400 (:status response)))
          (is (= {:message const/bad-request-error-message
                  :errors  ["Value is not valid"]}
                 body))
          (is (spy/not-called? presentations-db/get-all-presentations)))))))

(deftest delete-presentation-by-id-test
  (testing "Test DELETE /presentations/:presentation-id authorized with :admin role and valid `presentation-id`"
    (with-redefs [presentations-db/delete-presentation (spy/stub 1)]
      (let [app      (test-app/api-routes-with-auth)
            response (app (-> (mock/request :delete (str "/api/presentations/" test-presentation-id))
                              (mock/header :authorization (td/test-auth-token #{:admin}))))]
        (is (= 204 (:status response)))
        (is (empty? (:body response)))
        (is (spy/called-once-with? presentations-db/delete-presentation nil test-presentation-id)))))

  (doseq [role [:moderator :guest]]
    (testing (str "Test DELETE /presentations/:presentation-id authorized with " role " role and valid `presentation-id`")
      (with-redefs [presentations-db/delete-presentation (spy/spy)]
        (let [app      (test-app/api-routes-with-auth)
              response (app (-> (mock/request :delete (str "/api/presentations/" test-presentation-id))
                                (mock/header :authorization (td/test-auth-token #{role}))))
              body     (test-app/parse-body (:body response))]
          (is (= 403 (:status response)))
          (is (= {:message const/no-access-error-message} body))
          (is (spy/not-called? presentations-db/delete-presentation))))))

  (testing "Test DELETE /presentations/:presentation-id without authorization header"
    (with-redefs [presentations-db/delete-presentation (spy/spy)]
      (let [app      (test-app/api-routes-with-auth)
            response (app (mock/request :delete (str "/api/presentations/" test-presentation-id)))
            body     (test-app/parse-body (:body response))]
        (is (= 401 (:status response)))
        (is (= {:message const/not-authorized-error-message} body))
        (is (spy/not-called? presentations-db/delete-presentation)))))

  (testing "Test DELETE /presentations/:presentation-id for non-existing `presentation-id`"
    (with-redefs [presentations-db/delete-presentation (spy/stub 0)]
      (let [app      (test-app/api-routes-with-auth)
            response (app (-> (mock/request :delete (str "/api/presentations/" test-presentation-id))
                              (mock/header :authorization (td/test-auth-token #{:admin}))))
            body     (test-app/parse-body (:body response))]
        (is (= 404 (:status response)))
        (is (= {:message const/not-found-error-message} body))
        (is (spy/called-once-with? presentations-db/delete-presentation nil test-presentation-id)))))

  (testing "Test DELETE /presentations/:presentation-id with invalid `presentation-id`"
    (with-redefs [presentations-db/delete-presentation (spy/spy)]
      (let [app      (test-app/api-routes-with-auth)
            response (app (-> (mock/request :delete "/api/presentations/invalid")
                              (mock/header :authorization (td/test-auth-token #{:admin}))))
            body     (test-app/parse-body (:body response))]
        (is (= 400 (:status response)))
        (is (= {:message const/bad-request-error-message
                :errors  ["Value is not valid"]}
               body))
        (is (spy/not-called? presentations-db/delete-presentation)))))

  (testing "Test DELETE /presentations/:presentation-id unexpected result from database"
    (with-redefs [presentations-db/delete-presentation (spy/stub 2)]
      (let [app      (test-app/api-routes-with-auth)
            response (app (-> (mock/request :delete (str "/api/presentations/" test-presentation-id))
                              (mock/header :authorization (td/test-auth-token #{:admin}))))
            body     (test-app/parse-body (:body response))]
        (is (= 500 (:status response)))
        (is (= {:message const/server-error-message} body))
        (is (spy/called-once-with? presentations-db/delete-presentation nil test-presentation-id))))))
