(ns education.database.component-test
  (:require [clojure.test :refer [deftest is testing]]
            [education.config :as config]
            [education.database.component :as sut]
            [education.test-data :refer [test-config]]
            [next.jdbc.connection :as connection]
            [ragtime.jdbc :as jdbc]
            [ragtime.repl :as repl]
            [spy.core :as spy])
  (:import com.mchange.v2.c3p0.ComboPooledDataSource))

(defprotocol DummyConnection
  (close [this]))

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

(deftest migrate-test
  (testing "Test migrate"
    (with-redefs [config/config       (spy/stub test-config)
                  jdbc/sql-database   (spy/spy)
                  jdbc/load-resources (spy/spy)
                  repl/migrate        (spy/spy)]
      (sut/migrate "prod")
      (is (spy/called-once-with? config/config :prod))
      (is (spy/called-once-with? repl/migrate {:datastore nil :migrations nil})))))

(deftest rollback-test
  (testing "Test rollback"
    (with-redefs [config/config       (spy/stub test-config)
                  jdbc/sql-database   (spy/spy)
                  jdbc/load-resources (spy/spy)
                  repl/rollback       (spy/spy)]
      (sut/rollback "prod")
      (is (spy/called-once-with? config/config :prod))
      (is (spy/called-once-with? repl/rollback {:datastore nil :migrations nil})))))

(deftest map->Database-test
  (testing "Test start database component"
    (let [db-pool :db-pool]
      (with-redefs [connection/->pool (spy/stub db-pool)]
        (let [db-spec (config/db-spec test-config)
              c       (.start (.start (sut/map->Database {:db-spec db-spec})))]
          (is (= db-pool (:datasource c)))
          (is (= db-spec (:db-spec c)))
          (is (spy/called-once-with? connection/->pool ComboPooledDataSource db-spec))))))

  (testing "Test stop database component"
    (let [db-pool (reify DummyConnection (close [this] :db-pool))]
      (with-redefs [connection/->pool (spy/stub db-pool)]
        (let [db-spec (config/db-spec test-config)
              c       (.stop (.start (sut/map->Database {:db-spec db-spec})))]
          (is (= nil (:datasource c)))
          (is (= db-spec (:db-spec c))))))))

(deftest new-database-test
  (testing "Test creation database component using new-database function"
    (with-redefs [sut/map->Database (spy/spy)]
      (sut/new-database test-config)
      (is (spy/called-once-with? sut/map->Database {:db-spec (config/db-spec test-config)})))))
