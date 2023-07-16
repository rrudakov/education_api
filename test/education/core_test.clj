(ns education.core-test
  (:require
   [clojure.test :refer [deftest testing]]
   [education.core :as sut]
   [education.system :refer [system-config-prod]]
   [integrant.core :as ig]
   [spy.assert :as assert]
   [spy.core :as spy]))

(deftest main-test
  (testing "Test application start system with prod profile"
    (with-redefs [ig/init (spy/spy)]
      (sut/-main)
      (assert/called-once-with? ig/init system-config-prod))))
