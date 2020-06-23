(ns education.config-test
  (:require [buddy.auth.backends.token :refer [jws-backend]]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest testing is]]
            [education.config :as sut]
            [spy.core :as spy]))

(def parsed-configs
  "Define parsed config."
  {:dev
   {:database
    {:url "test_database_url"}
    :app {:port      3000
          :tokensign "not_super_secure_token"}}
   :prod
   {:database
    {:url "prod_database_url"}
    :app {:port      3000
          :tokensign "super_secure_token"}}})

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
        (is (= {:jdbcUrl                      (get-in config [:database :url])
                :maxIdleTime                  10800
                :maxIdleTimeExcessConnections 10800
                :maxPoolSize                  30}
               (sut/db-spec config)))))))
