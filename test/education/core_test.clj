(ns education.core-test
  (:require [clojure.test :refer [deftest is testing]]
            [education.core :as sut]
            [education.system :refer [system-config-prod]]
            [integrant.core :as ig]
            [spy.core :as spy]))

(deftest main-test
  (testing "Test application start system with prod profile"
    (with-redefs [ig/init (spy/spy)]
      (sut/-main)
      (is (spy/called-once-with? ig/init system-config-prod)))))
