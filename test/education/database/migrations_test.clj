(ns education.database.migrations-test
  (:require [clojure.test :refer [deftest is testing]]
            [education.config :as config]
            [education.database.migrations :as sut]
            [education.test-data :refer [test-config]]
            [ragtime.jdbc :as jdbc]
            [ragtime.repl :as repl]
            [spy.core :as spy]))

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
          (is (contains? db-conf :datastore))
          (is (contains? db-conf :migrations)))))))

(deftest migrate-test
  (testing "Test migrate"
    (with-redefs [config/config       (spy/stub test-config)
                  jdbc/sql-database   (spy/spy)
                  jdbc/load-resources (spy/spy)
                  repl/migrate        (spy/spy)]
      (sut/migrate {:profile :prod})
      (is (spy/called-once-with? config/config :prod))
      (is (spy/called-once-with? repl/migrate {:datastore nil :migrations nil})))))

(deftest rollback-test
  (testing "Test rollback"
    (with-redefs [config/config       (spy/stub test-config)
                  jdbc/sql-database   (spy/spy)
                  jdbc/load-resources (spy/spy)
                  repl/rollback       (spy/spy)]
      (sut/rollback {:profile :prod})
      (is (spy/called-once-with? config/config :prod))
      (is (spy/called-once-with? repl/rollback {:datastore nil :migrations nil})))))
