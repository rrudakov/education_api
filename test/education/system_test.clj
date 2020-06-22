(ns education.system-test
  (:require [clojure.test :refer [deftest testing is]]
            [education.config :as config]
            [education.database.component :refer [new-database]]
            [education.http.component :refer [new-webserver]]
            [education.system :as sut]
            [education.test-data :refer [test-config]]
            [spy.core :as spy]
            [com.stuartsierra.component :as component]))

(deftest api-system-test
  (testing "Test api-system creation"
    (with-redefs [config/config (spy/stub test-config)
                  new-database  (spy/stub :new-database)
                  new-webserver (spy/stub :new-webserver)]
      (let [s (sut/api-system :prod)]
        (is (spy/called-once-with? config/config :prod))
        (is (spy/called-once-with? new-database test-config))
        (is (spy/called-once-with? new-webserver test-config))
        (is (= (component/system-map :db :new-database :http-server :new-webserver) s))))))
