(ns education.database.migrations-test
  (:require
   [clojure.test :refer [deftest testing]]
   [education.config :as config]
   [education.database.migrations :as sut]
   [education.test-data :refer [test-config]]
   [hikari-cp.core :refer [close-datasource make-datasource]]
   [ragtime.next-jdbc]
   [ragtime.repl :as repl]
   [ragtime.strategy :as strategy]
   [spy.assert :as assert]
   [spy.core :as spy]))

(deftest migrate-test
  (testing "Test migrate"
    (with-redefs [config/config                    (spy/stub test-config)
                  make-datasource                  (spy/spy)
                  close-datasource                 (spy/spy)
                  ragtime.next-jdbc/load-resources (spy/spy)
                  ragtime.next-jdbc/sql-database   (spy/spy)
                  repl/migrate                     (spy/spy)]
      (sut/migrate {:profile :prod})
      (assert/called-once-with? config/config :prod)
      (assert/called-once-with? make-datasource
                                (config/db-spec test-config))
      (assert/called-once-with? ragtime.next-jdbc/load-resources
                                "migrations")
      (assert/called-once-with? ragtime.next-jdbc/sql-database nil)
      (assert/called-once-with? repl/migrate
                                {:datastore  nil
                                 :migrations nil
                                 :strategy   strategy/apply-new}))))

(deftest rollback-test
  (testing "Test rollback"
    (with-redefs [config/config                    (spy/stub test-config)
                  make-datasource                  (spy/spy)
                  close-datasource                 (spy/spy)
                  ragtime.next-jdbc/load-resources (spy/spy)
                  ragtime.next-jdbc/sql-database   (spy/spy)
                  repl/rollback                    (spy/spy)]
      (sut/rollback {:profile :prod})
      (assert/called-once-with? config/config :prod)
      (assert/called-once-with? make-datasource
                                (config/db-spec test-config))
      (assert/called-once-with? ragtime.next-jdbc/load-resources
                                "migrations")
      (assert/called-once-with? ragtime.next-jdbc/sql-database nil)
      (assert/called-once-with? repl/rollback
                                {:datastore  nil
                                 :migrations nil
                                 :strategy   strategy/apply-new}))))
