(ns education.utils.path-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [education.utils.path :as sut]))

(deftest join-test
  (testing "Test join a few strings without / characters"
    (is (= "some/path/to/some/directory" (sut/join "some" "path" "to" "some" "directory"))))

  (testing "Test join a few strings starts and ends with / characters"
    (is (= "/some/path/to/some/directory/" (sut/join "/some/" "/path/" "/to/" "/some/" "/directory/"))))

  (testing "Test join with a single string as argument"
    (is (= "/some" (sut/join " /some/ "))))

  (testing "Test join a few line starts and ends with spaces"
    (is (= "/some/path/to/directory" (sut/join " /some/ " " path " " /to" "/directory ")))))
