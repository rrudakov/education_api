(ns education.database.component-test
  (:require [clojure.test :refer :all]
            [education.config :as config]
            [education.database.component :as sut]
            [education.test-data :refer :all]
            [spy.core :as spy]
            [ragtime.jdbc :as jdbc]))

(deftest load-db-config-test
  (testing "Test loading database config"
    (let [profile :prod]
      (with-redefs [config/config       (spy/stub test-config)
                    jdbc/sql-database   (spy/spy)
                    jdbc/load-resources (spy/spy)]
        (let [db-conf (sut/load-db-config profile)]
          (is (spy/called-once-with? jdbc/sql-database
                                     {:connection-uri (:url (config/db-config test-config))}))
          (is (spy/called-once-with? jdbc/load-resources "migrations"))
          (is (= [:datastore :migrations] (keys db-conf))))))))

(deftest migrate-test)

(deftest rollback-test)
