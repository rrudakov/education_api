(ns education.core-test
  (:require [clojure.test :refer [deftest is testing]]
            [com.stuartsierra.component :as component]
            [education.core :as sut]
            [education.system :refer [api-system]]
            [spy.core :as spy]))

(deftest main-test
  (testing "Test application start system with prod profile"
    (with-redefs [component/start (spy/stub)
                  api-system (spy/stub)]
      (sut/-main)
      (is (spy/called-once? component/start))
      (is (spy/called-once-with? api-system :prod)))))
