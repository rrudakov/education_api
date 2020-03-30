(ns ring-learn.core-test
  (:require [clojure.test :refer :all]
            [ring-learn.core :refer :all]
            [ring.mock.request :as mock]))

;; (deftest simple-body-page-test
;;   (testing "Default handler."
;;     (is (= (app-routes (mock/request :get "/"))
;;            {:status 200
;;             :headers {"Content-Type" "text/html"}
;;             :body "Hello World!"}))))

;; (deftest request-example-test
;;   (testing "Request details test."
;;     (let [req (mock/request :get "/request")]
;;       (is (= (app-routes req)
;;              {:status 200
;;               :headers {"Content-Type" "text/html"}
;;               :body (str "Request object: " req)})))))
