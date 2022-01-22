(ns education.config-test
  (:require [buddy.auth.backends.token :refer [jws-backend]]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest testing is]]
            [education.config :as sut]
            [spy.core :as spy]
            [buddy.core.hash :as hash]
            [buddy.core.codecs :as codecs]))

(def parsed-configs
  "Define parsed config."
  {:dev
   {:database
    {:url "test_database_url"}
    :app {:port          3000
          :tokensign     "not_super_secure_token"
          :base_url      "http://127.0.0.1:3000"
          :storage       "/tmp/"
          :crypto        {:key "key"
                          :iv  "iv"}
          :send-grid     {:base-url  "https://api.url"
                          :api-key   "api-key"
                          :templates {:free-lesson "free-lesson-template-id"}}
          :video-lessons {:root-path        "/some/storage"
                          :free-lesson-path "zima.mp4"}}}
   :prod
   {:database
    {:url "prod_database_url"}
    :app {:port      3000
          :tokensign "super_secure_token"
          :base_url  "https://prod.url"
          :storage   "/prod/storage"

          :crypto        {:key "key"
                          :iv  "iv"}
          :send-grid     {:base-url  "https://api.url"
                          :api-key   "api-key"
                          :templates {:free-lesson "free-lesson-template-id"}}
          :video-lessons {:root-path        "/some/storage"
                          :free-lesson-path "zima.mp4"}}}})

(def raw-config
  "Define raw test configuration."
  (io/resource "config/config_test.edn"))

(deftest config-test-read-config-for-profile
  (doseq [profile [:dev :prod]]
    (testing "Test parse config for appropriate profile"
      (with-redefs [io/resource (spy/stub raw-config)]
        (is (= (get parsed-configs profile) (sut/config profile)))
        (is (spy/called-once-with? io/resource "config/config.edn"))))))

(deftest application-port-test
  (doseq [profile [:dev :prod]]
    (testing "Test getting application port from config"
      (let [config (get parsed-configs profile)]
        (is (= (get-in config [:app :port]) (sut/application-port config)))))))

(deftest token-sign-secret-test
  (doseq [profile [:dev :prod]]
    (testing "Test getting token sign secret from config"
      (let [config (get parsed-configs profile)]
        (is (= (get-in config [:app :tokensign]) (sut/token-sign-secret config)))))))

(deftest auth-backend-test
  (doseq [profile [:dev :prod]]
    (testing "Test getting authorization backend from config"
      (with-redefs [jws-backend (spy/spy)]
        (let [config          (get parsed-configs profile)
              expected-params {:secret     (get-in config [:app :tokensign])
                               :token-name "Token"
                               :options    {:alg :hs512}}]
          (sut/auth-backend config)
          (is (spy/called-once-with? jws-backend expected-params)))))))

(deftest db-config-test
  (doseq [profile [:dev :prod]]
    (testing "Test getting database configuration from config"
      (let [config (get parsed-configs profile)]
        (is (= (:database config) (sut/db-config config)))))))

(deftest db-spec-test
  (doseq [profile [:dev :prod]]
    (testing "Test getting database specification from config"
      (let [config (get parsed-configs profile)]
        (is (= {:jdbc-url      (get-in config [:database :url])
                :pool-name     "education-pool"
                :max-pool-size 30}
               (sut/db-spec config)))))))

(deftest base-url-test
  (doseq [profile [:dev :prod]]
    (testing "Test getting base URL from config"
      (let [config (profile parsed-configs)]
        (is (= (get-in config [:app :base_url]) (sut/base-url config)))))))

(deftest storage-path-test
  (doseq [profile [:dev :prod]]
    (testing "Test getting storage path from config"
      (let [config (profile parsed-configs)]
        (is (= (get-in config [:app :storage]) (sut/storage-path config)))))))

(deftest crypto-key-test
  (doseq [profile [:dev :prod]]
    (testing "Test getting crypto key from config"
      (with-redefs [hash/sha256 (spy/stub "value")]
        (let [config (profile parsed-configs)]
          (is (= "value" (sut/crypto-key config)))
          (is (spy/called-once-with? hash/sha256 (get-in config [:app :crypto :key]))))))))

(deftest crypto-iv-test
  (doseq [profile [:dev :prod]]
    (testing "Test getting crypto iv from config"
      (with-redefs [codecs/str->bytes (spy/stub "value")]
        (let [config (profile parsed-configs)]
          (is (= "value" (sut/crypto-iv config)))
          (is (spy/called-once-with? codecs/str->bytes (get-in config [:app :crypto :iv]))))))))

(deftest send-grid-base-url-test
  (doseq [profile [:dev :prod]]
    (testing "Test getting send-grid base URL from config"
      (let [config (profile parsed-configs)]
        (is (= (get-in config [:app :send-grid :base-url]) (sut/send-grid-base-url config)))))))

(deftest send-grid-api-key-test
  (doseq [profile [:dev :prod]]
    (testing "Test getting send-grid API key from config"
      (let [config (profile parsed-configs)]
        (is (= (get-in config [:app :send-grid :api-key]) (sut/send-grid-api-key config)))))))

(deftest free-lesson-template-id-test
  (doseq [profile [:dev :prod]]
    (testing "Test getting send-grid free lesson template from config"
      (let [config (profile parsed-configs)]
        (is (= (get-in config [:app :send-grid :templates :free-lesson]) (sut/free-lesson-template-id config)))))))

(deftest video-lessons-root-path-test
  (doseq [profile [:dev :prod]]
    (testing "Test getting root path for video lessons files from config"
      (let [config (profile parsed-configs)]
        (is (= (get-in config [:app :video-lessons :root-path]) (sut/video-lessons-root-path config)))))))

(deftest free-lesson-path-test
  (doseq [profile [:dev :prod]]
    (testing "Test getting free lesson path from config"
      (let [config (profile parsed-configs)]
        (is (= (get-in config [:app :video-lessons :free-lesson-path]) (sut/free-lesson-path config)))))))
